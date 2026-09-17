from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.api.dependencies import (
    PatientAccessor,
    get_accessible_patient_for_accessor,
    get_current_caregiver,
    get_current_patient_accessor,
    get_db,
)
from app.identifiers import generate_patient_public_id
from app.models.caregiver import Caregiver
from app.models.game import Game
from app.models.game_result import GameResult
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.models.performance_metric import PerformanceMetric
from app.models.reminder import Reminder
from app.models.user import User
from app.schemas.dashboard import (
    CaregiverRelationship,
    DashboardResponse,
    PatientCreateRequest,
    PatientLinkRequest,
    PatientProfile,
    PatientSummary,
    PatientsResponse,
    PerformanceHistoryResponse,
    PerformancePoint,
    RecentGameSession,
    ReminderItem,
    SessionResult,
)
from app.schemas.reminder import ReminderCreateRequest

router = APIRouter(tags=["caregiver dashboard"])

CAREGIVER_ACCESS_DESCRIPTION = (
    "Requires a Bearer access token for an authenticated caregiver. "
    "Caregiver identity is taken from the JWT, not from a client-supplied caregiver ID."
)


@router.get(
    "/patients",
    response_model=PatientsResponse,
    summary="List patients available to the authenticated caregiver",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def list_patients(
    accessor: PatientAccessor = Depends(get_current_patient_accessor),
    db: Session = Depends(get_db),
) -> PatientsResponse:
    rows = db.execute(
        select(Patient, User.display_name)
        .join(User, User.id == Patient.user_id)
        .join(PatientCaregiver, PatientCaregiver.patient_id == Patient.id)
        .where(PatientCaregiver.caregiver_id == accessor.caregiver.id)
        .order_by(User.display_name, Patient.id)
    ).all()

    return PatientsResponse(
        patients=[
            PatientSummary(
                id=patient.id,
                public_id=patient.public_id,
                display_name=display_name,
                preferred_language=patient.preferred_language,
                timezone=patient.timezone,
                profile_photo_ref=patient.profile_photo_ref,
            )
            for patient, display_name in rows
        ]
    )


@router.post(
    "/patients",
    response_model=PatientProfile,
    status_code=status.HTTP_201_CREATED,
    summary="Create a new patient and become their primary caregiver",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def create_patient(
    payload: PatientCreateRequest,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> PatientProfile:
    user = User(
        display_name=payload.display_name.strip(),
        role="PATIENT",
        is_active=True,
    )
    db.add(user)
    db.flush()

    public_id = None
    for _ in range(10):
        candidate = generate_patient_public_id()
        exists = db.scalar(select(Patient.id).where(Patient.public_id == candidate))
        if not exists:
            public_id = candidate
            break
    if not public_id:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to generate unique patient public ID",
        )

    patient = Patient(
        user_id=user.id,
        public_id=public_id,
        date_of_birth=payload.date_of_birth,
        gender=payload.gender,
        preferred_language=payload.preferred_language,
        timezone=payload.timezone,
        profile_photo_ref=payload.profile_photo_ref,
    )
    db.add(patient)
    db.flush()

    link = PatientCaregiver(
        patient_id=patient.id,
        caregiver_id=caregiver.id,
        is_primary=True,
    )
    db.add(link)
    db.commit()
    db.refresh(patient)

    return PatientProfile(
        id=patient.id,
        public_id=patient.public_id,
        display_name=user.display_name,
        preferred_language=patient.preferred_language,
        timezone=patient.timezone,
        profile_photo_ref=patient.profile_photo_ref,
        date_of_birth=patient.date_of_birth,
        gender=patient.gender,
    )


@router.post(
    "/patients/link",
    response_model=PatientProfile,
    status_code=status.HTTP_200_OK,
    summary="Link an existing patient by public ID as a secondary caregiver",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def link_patient(
    payload: PatientLinkRequest,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> PatientProfile:
    normalized_public_id = payload.public_id.strip().upper()
    patient = db.scalar(select(Patient).where(Patient.public_id == normalized_public_id))
    if patient is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found with the provided public ID",
        )

    existing_link = db.scalar(
        select(PatientCaregiver).where(
            PatientCaregiver.patient_id == patient.id,
            PatientCaregiver.caregiver_id == caregiver.id,
        )
    )
    if existing_link is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Patient is already linked to this caregiver",
        )

    link = PatientCaregiver(
        patient_id=patient.id,
        caregiver_id=caregiver.id,
        is_primary=False,
    )
    db.add(link)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Patient is already linked to this caregiver",
        ) from None

    user = db.scalar(select(User).where(User.id == patient.user_id))
    return PatientProfile(
        id=patient.id,
        public_id=patient.public_id,
        display_name=user.display_name if user else "",
        preferred_language=patient.preferred_language,
        timezone=patient.timezone,
        profile_photo_ref=patient.profile_photo_ref,
        date_of_birth=patient.date_of_birth,
        gender=patient.gender,
    )


@router.get(
    "/patients/{patient_id}/dashboard",
    response_model=DashboardResponse,
    summary="Get caregiver dashboard data for one patient",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def get_patient_dashboard(
    patient_id: UUID,
    accessor: PatientAccessor = Depends(get_current_patient_accessor),
    db: Session = Depends(get_db),
) -> DashboardResponse:
    patient = get_accessible_patient_for_accessor(patient_id, accessor, db)
    display_name = db.scalar(select(User.display_name).where(User.id == patient.user_id))

    relationship_row = db.execute(
        select(PatientCaregiver, Caregiver, User.display_name)
        .join(Caregiver, Caregiver.id == PatientCaregiver.caregiver_id)
        .join(User, User.id == Caregiver.user_id)
        .where(
            PatientCaregiver.patient_id == patient_id,
            PatientCaregiver.caregiver_id == accessor.caregiver.id,
        )
    ).one_or_none()

    caregiver_relationship = None
    if relationship_row is not None:
        link, caregiver, caregiver_name = relationship_row
        caregiver_relationship = CaregiverRelationship(
            caregiver_id=caregiver.id,
            public_id=caregiver.public_id,
            display_name=caregiver_name,
            caregiver_type=caregiver.caregiver_type,
            is_primary=link.is_primary,
        )

    session_rows = db.execute(
        select(GameSession, Game, GameResult)
        .join(Game, Game.id == GameSession.game_id)
        .outerjoin(GameResult, GameResult.session_id == GameSession.id)
        .where(GameSession.patient_id == patient_id)
        .order_by(GameSession.started_at.desc())
        .limit(10)
    ).all()

    recent_sessions = [
        RecentGameSession(
            id=session.id,
            game_id=game.id,
            game_code=game.code,
            game_name=game.name,
            started_at=session.started_at,
            completed_at=session.completed_at,
            status=session.status,
            difficulty_level=session.difficulty_level,
            result=(
                SessionResult(
                    score=result.score,
                    accuracy=result.accuracy,
                    correct_answers=result.correct_answers,
                    total_questions=result.total_questions,
                    response_time_ms=result.response_time_ms,
                    mistakes=result.mistakes,
                )
                if result is not None
                else None
            ),
        )
        for session, game, result in session_rows
    ]

    latest_metric = db.scalar(
        select(PerformanceMetric)
        .where(PerformanceMetric.patient_id == patient_id)
        .order_by(PerformanceMetric.metric_date.desc())
        .limit(1)
    )

    reminders = db.scalars(
        select(Reminder)
        .where(
            Reminder.patient_id == patient_id,
            Reminder.is_active.is_(True),
            Reminder.scheduled_at >= func.now(),
        )
        .order_by(Reminder.scheduled_at)
    ).all()

    return DashboardResponse(
        patient=PatientProfile(
            id=patient.id,
            public_id=patient.public_id,
            display_name=display_name,
            preferred_language=patient.preferred_language,
            timezone=patient.timezone,
            profile_photo_ref=patient.profile_photo_ref,
            date_of_birth=patient.date_of_birth,
            gender=patient.gender,
        ),
        caregiver_relationship=caregiver_relationship,
        recent_sessions=recent_sessions,
        latest_performance=(
            PerformancePoint.model_validate(latest_metric) if latest_metric is not None else None
        ),
        active_reminders=[
            ReminderItem(
                id=reminder.id,
                title=reminder.title,
                description=reminder.description,
                reminder_type=reminder.reminder_type,
                scheduled_at=reminder.scheduled_at,
                is_recurring=reminder.is_recurring,
                recurrence_rule=reminder.recurrence_rule,
            )
            for reminder in reminders
        ],
    )


@router.get(
    "/patients/{patient_id}/performance",
    response_model=PerformanceHistoryResponse,
    summary="Get daily performance history for a patient",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def get_patient_performance(
    patient_id: UUID,
    accessor: PatientAccessor = Depends(get_current_patient_accessor),
    db: Session = Depends(get_db),
) -> PerformanceHistoryResponse:
    get_accessible_patient_for_accessor(patient_id, accessor, db)
    metrics = db.scalars(
        select(PerformanceMetric)
        .where(PerformanceMetric.patient_id == patient_id)
        .order_by(PerformanceMetric.metric_date.asc())
    ).all()

    return PerformanceHistoryResponse(
        patient_id=patient_id,
        metrics=[PerformancePoint.model_validate(metric) for metric in metrics],
    )


def _session_response(
    session: GameSession,
    game: Game,
    result: GameResult | None,
) -> RecentGameSession:
    return RecentGameSession(
        id=session.id,
        game_id=game.id,
        game_code=game.code,
        game_name=game.name,
        started_at=session.started_at,
        completed_at=session.completed_at,
        status=session.status,
        difficulty_level=session.difficulty_level,
        result=(
            SessionResult(
                score=result.score,
                accuracy=result.accuracy,
                correct_answers=result.correct_answers,
                total_questions=result.total_questions,
                response_time_ms=result.response_time_ms,
                mistakes=result.mistakes,
            )
            if result is not None
            else None
        ),
    )


@router.get(
    "/patients/{patient_id}/sessions",
    response_model=list[RecentGameSession],
    summary="List a patient's game-session history",
)
def get_patient_sessions(
    patient_id: UUID,
    limit: int = Query(default=50, ge=1, le=100),
    accessor: PatientAccessor = Depends(get_current_patient_accessor),
    db: Session = Depends(get_db),
) -> list[RecentGameSession]:
    get_accessible_patient_for_accessor(patient_id, accessor, db)
    rows = db.execute(
        select(GameSession, Game, GameResult)
        .join(Game, Game.id == GameSession.game_id)
        .outerjoin(GameResult, GameResult.session_id == GameSession.id)
        .where(GameSession.patient_id == patient_id)
        .order_by(GameSession.started_at.desc())
        .limit(limit)
    ).all()
    return [_session_response(session, game, result) for session, game, result in rows]


@router.get(
    "/patients/{patient_id}/trends",
    response_model=PerformanceHistoryResponse,
    summary="List a patient's performance trend history",
)
def get_patient_trends(
    patient_id: UUID,
    accessor: PatientAccessor = Depends(get_current_patient_accessor),
    db: Session = Depends(get_db),
) -> PerformanceHistoryResponse:
    get_accessible_patient_for_accessor(patient_id, accessor, db)
    metrics = db.scalars(
        select(PerformanceMetric)
        .where(PerformanceMetric.patient_id == patient_id)
        .order_by(PerformanceMetric.metric_date.asc())
    ).all()
    return PerformanceHistoryResponse(
        patient_id=patient_id,
        metrics=[PerformancePoint.model_validate(metric) for metric in metrics],
    )


@router.get(
    "/patients/{patient_id}/reminders",
    response_model=list[ReminderItem],
    summary="List a patient's reminders",
)
def get_patient_reminders(
    patient_id: UUID,
    accessor: PatientAccessor = Depends(get_current_patient_accessor),
    db: Session = Depends(get_db),
) -> list[ReminderItem]:
    get_accessible_patient_for_accessor(patient_id, accessor, db)
    reminders = db.scalars(
        select(Reminder)
        .where(Reminder.patient_id == patient_id)
        .order_by(Reminder.scheduled_at.asc())
    ).all()
    return [_reminder_response(reminder) for reminder in reminders]


def _reminder_response(reminder: Reminder) -> ReminderItem:
    return ReminderItem(
        id=reminder.id,
        title=reminder.title,
        description=reminder.description,
        reminder_type=reminder.reminder_type,
        scheduled_at=reminder.scheduled_at,
        is_recurring=reminder.is_recurring,
        recurrence_rule=reminder.recurrence_rule,
    )


@router.post(
    "/patients/{patient_id}/reminders",
    response_model=ReminderItem,
    status_code=status.HTTP_201_CREATED,
    summary="Create a reminder for an assigned patient",
)
def create_patient_reminder(
    patient_id: UUID,
    reminder_data: ReminderCreateRequest,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> ReminderItem:
    from app.api.dependencies import get_accessible_patient

    get_accessible_patient(patient_id, caregiver, db)
    reminder = Reminder(patient_id=patient_id, **reminder_data.model_dump())
    db.add(reminder)
    db.commit()
    db.refresh(reminder)
    return _reminder_response(reminder)
