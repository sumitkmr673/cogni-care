import secrets
from datetime import datetime, timezone
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.api.dependencies import (
    PatientAccessor,
    get_accessible_patient,
    get_accessible_patient_for_accessor,
    get_current_caregiver,
    get_current_patient_accessor,
    get_db,
)
from app.identifiers import generate_device_identifier, generate_patient_public_id
from app.models.caregiver import Caregiver
from app.models.game import Game
from app.models.game_result import GameResult
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.models.patient_device import PatientDevice
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
from app.schemas.analysis import PatientAnalysisResponse
from app.schemas.device import (
    DeviceProvisionRequest,
    DeviceProvisionResponse,
    DeviceRevokeResponse,
    DeviceSummaryResponse,
)
from app.schemas.reminder import (
    ReminderCreateRequest,
    ReminderStatusUpdateRequest,
    ReminderUpdateRequest,
)
from app.services.analysis import analyze_patient_performance
from app.security import hash_password


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
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> PatientsResponse:
    rows = db.execute(
        select(Patient, User.display_name)
        .join(User, User.id == Patient.user_id)
        .join(PatientCaregiver, PatientCaregiver.patient_id == Patient.id)
        .where(PatientCaregiver.caregiver_id == caregiver.id)
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

    has_primary = db.scalar(
        select(PatientCaregiver.id).where(
            PatientCaregiver.patient_id == patient.id,
            PatientCaregiver.is_primary.is_(True),
        )
    )
    link = PatientCaregiver(
        patient_id=patient.id,
        caregiver_id=caregiver.id,
        is_primary=(has_primary is None),
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

    reminder_rows = db.execute(
        select(Reminder, Caregiver.public_id, User.display_name)
        .join(Caregiver, Caregiver.id == Reminder.created_by_caregiver_id)
        .join(User, User.id == Caregiver.user_id)
        .where(
            Reminder.patient_id == patient_id,
            Reminder.is_active.is_(True),
            Reminder.scheduled_at >= func.now(),
        )
        .order_by(Reminder.scheduled_at)
        .limit(5)
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
            _reminder_response(reminder, pub_id, name)
            for reminder, pub_id, name in reminder_rows
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
    "/patients/{patient_id}/analysis",
    response_model=PatientAnalysisResponse,
    summary="Get rule-based performance analysis and observations for a patient",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def get_patient_analysis(
    patient_id: UUID,
    accessor: PatientAccessor = Depends(get_current_patient_accessor),
    db: Session = Depends(get_db),
) -> PatientAnalysisResponse:
    get_accessible_patient_for_accessor(patient_id, accessor, db)
    return analyze_patient_performance(db, patient_id)


def _reminder_response(
    reminder: Reminder,
    creator_public_id: str | None = None,
    creator_display_name: str | None = None,
) -> ReminderItem:
    return ReminderItem(
        id=reminder.id,
        title=reminder.title,
        description=reminder.description,
        reminder_type=reminder.reminder_type,
        scheduled_at=reminder.scheduled_at,
        is_recurring=reminder.is_recurring,
        recurrence_rule=reminder.recurrence_rule,
        is_active=reminder.is_active,
        created_at=reminder.created_at,
        created_by_caregiver_public_id=creator_public_id,
        created_by_display_name=creator_display_name,
    )


def _get_reminder_with_auth(
    patient_id: UUID,
    reminder_id: UUID,
    caregiver: Caregiver,
    db: Session,
    require_manage: bool = False,
) -> tuple[Reminder, str, str | None]:
    get_accessible_patient(patient_id, caregiver, db)

    row = db.execute(
        select(Reminder, Caregiver.public_id, User.display_name)
        .join(Caregiver, Caregiver.id == Reminder.created_by_caregiver_id)
        .join(User, User.id == Caregiver.user_id)
        .where(Reminder.id == reminder_id, Reminder.patient_id == patient_id)
    ).first()

    if row is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Reminder not found",
        )

    reminder, creator_public_id, creator_name = row

    if require_manage:
        link = db.scalar(
            select(PatientCaregiver).where(
                PatientCaregiver.patient_id == patient_id,
                PatientCaregiver.caregiver_id == caregiver.id,
            )
        )
        is_primary = bool(link and link.is_primary)
        is_creator = (reminder.created_by_caregiver_id == caregiver.id)

        if not (is_primary or is_creator):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You do not have permission to modify this reminder",
            )

    return reminder, creator_public_id, creator_name


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
    rows = db.execute(
        select(Reminder, Caregiver.public_id, User.display_name)
        .join(Caregiver, Caregiver.id == Reminder.created_by_caregiver_id)
        .join(User, User.id == Caregiver.user_id)
        .where(Reminder.patient_id == patient_id)
        .order_by(Reminder.scheduled_at.asc())
    ).all()
    return [_reminder_response(reminder, pub_id, name) for reminder, pub_id, name in rows]


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
    get_accessible_patient(patient_id, caregiver, db)
    reminder = Reminder(
        patient_id=patient_id,
        created_by_caregiver_id=caregiver.id,
        **reminder_data.model_dump(),
    )
    db.add(reminder)
    db.commit()
    db.refresh(reminder)
    user = db.scalar(select(User).where(User.id == caregiver.user_id))
    creator_name = user.display_name if user else None
    return _reminder_response(reminder, caregiver.public_id, creator_name)


@router.get(
    "/patients/{patient_id}/reminders/{reminder_id}",
    response_model=ReminderItem,
    summary="Get a single reminder",
)
def get_patient_reminder(
    patient_id: UUID,
    reminder_id: UUID,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> ReminderItem:
    reminder, creator_public_id, creator_name = _get_reminder_with_auth(
        patient_id, reminder_id, caregiver, db, require_manage=False
    )
    return _reminder_response(reminder, creator_public_id, creator_name)


@router.patch(
    "/patients/{patient_id}/reminders/{reminder_id}",
    response_model=ReminderItem,
    summary="Update a reminder",
)
def update_patient_reminder(
    patient_id: UUID,
    reminder_id: UUID,
    reminder_data: ReminderUpdateRequest,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> ReminderItem:
    reminder, creator_public_id, creator_name = _get_reminder_with_auth(
        patient_id, reminder_id, caregiver, db, require_manage=True
    )

    update_dict = reminder_data.model_dump(exclude_unset=True)
    update_dict.pop("patient_id", None)
    update_dict.pop("created_by_caregiver_id", None)

    # When switching to non-recurring, ensure recurrence_rule is cleared to null
    if update_dict.get("is_recurring") is False:
        if "recurrence_rule" not in update_dict or update_dict["recurrence_rule"] is None:
            update_dict["recurrence_rule"] = None

    for field, value in update_dict.items():
        setattr(reminder, field, value)

    if reminder.is_recurring and not reminder.recurrence_rule:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="recurrence_rule is required for recurring reminders",
        )
    if not reminder.is_recurring and reminder.recurrence_rule is not None:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="recurrence_rule must be null for non-recurring reminders",
        )

    db.commit()
    db.refresh(reminder)
    return _reminder_response(reminder, creator_public_id, creator_name)


@router.delete(
    "/patients/{patient_id}/reminders/{reminder_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a reminder",
)
def delete_patient_reminder(
    patient_id: UUID,
    reminder_id: UUID,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
):
    reminder, _, _ = _get_reminder_with_auth(
        patient_id, reminder_id, caregiver, db, require_manage=True
    )
    db.delete(reminder)
    db.commit()
    return None


@router.patch(
    "/patients/{patient_id}/reminders/{reminder_id}/status",
    response_model=ReminderItem,
    summary="Toggle reminder active status",
)
def update_reminder_status(
    patient_id: UUID,
    reminder_id: UUID,
    status_data: ReminderStatusUpdateRequest,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> ReminderItem:
    reminder, creator_public_id, creator_name = _get_reminder_with_auth(
        patient_id, reminder_id, caregiver, db, require_manage=True
    )
    reminder.is_active = status_data.is_active
    db.commit()
    db.refresh(reminder)
    return _reminder_response(reminder, creator_public_id, creator_name)


def _get_primary_caregiver_link(patient_id: UUID, caregiver: Caregiver, db: Session) -> PatientCaregiver:
    patient = db.scalar(select(Patient).where(Patient.id == patient_id))
    if patient is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found",
        )

    link = db.scalar(
        select(PatientCaregiver).where(
            PatientCaregiver.patient_id == patient_id,
            PatientCaregiver.caregiver_id == caregiver.id,
        )
    )
    if link is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found",
        )
    if not link.is_primary:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the primary caregiver can manage patient devices",
        )
    return link


@router.post(
    "/patients/{patient_id}/devices",
    response_model=DeviceProvisionResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Provision a device binding for a patient",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def provision_patient_device(
    patient_id: UUID,
    payload: DeviceProvisionRequest,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> DeviceProvisionResponse:
    _get_primary_caregiver_link(patient_id, caregiver, db)

    patient = db.scalar(select(Patient).where(Patient.id == patient_id))
    if patient is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found",
        )

    # Enforce one active device per patient
    existing_active = db.scalar(
        select(PatientDevice).where(
            PatientDevice.patient_id == patient_id,
            PatientDevice.status == "ACTIVE",
        )
    )
    if existing_active is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Patient already has an active device registered",
        )

    # If client device ID is supplied, check if that client is already active for another patient
    client_device_id = payload.client_device_id.strip() if payload.client_device_id else None
    if client_device_id:
        existing_client = db.scalar(
            select(PatientDevice).where(
                PatientDevice.client_device_id == client_device_id,
                PatientDevice.status == "ACTIVE",
            )
        )
        if existing_client is not None:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="This device is already active for another patient",
            )

    device_identifier = generate_device_identifier()
    device_key = secrets.token_urlsafe(32)

    device = PatientDevice(
        patient_id=patient_id,
        device_identifier=device_identifier,
        client_device_id=client_device_id,
        device_name=payload.device_name.strip() if payload.device_name else None,
        device_key_hash=hash_password(device_key),
        status="ACTIVE",
    )
    db.add(device)
    db.commit()
    db.refresh(device)

    user = db.scalar(select(User).where(User.id == patient.user_id))
    display_name = user.display_name if user else "Patient"

    return DeviceProvisionResponse(
        device_id=device.id,
        device_identifier=device.device_identifier,
        device_key=device_key,
        patient_id=patient.id,
        patient_public_id=patient.public_id,
        display_name=display_name,
        status=device.status,
        created_at=device.created_at,
    )


@router.get(
    "/patients/{patient_id}/devices",
    response_model=list[DeviceSummaryResponse],
    summary="List device bindings for a patient",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def list_patient_devices(
    patient_id: UUID,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> list[DeviceSummaryResponse]:
    get_accessible_patient(patient_id, caregiver, db)

    devices = db.scalars(
        select(PatientDevice)
        .where(PatientDevice.patient_id == patient_id)
        .order_by(PatientDevice.created_at.desc())
    ).all()

    return [
        DeviceSummaryResponse(
            device_id=d.id,
            device_identifier=d.device_identifier,
            device_name=d.device_name,
            client_device_id=d.client_device_id,
            status=d.status,
            created_at=d.created_at,
            last_seen_at=d.last_seen_at,
            revoked_at=d.revoked_at,
        )
        for d in devices
    ]


@router.post(
    "/patients/{patient_id}/devices/revoke",
    response_model=DeviceRevokeResponse,
    summary="Revoke the active device binding for a patient",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
@router.delete(
    "/patients/{patient_id}/devices",
    response_model=DeviceRevokeResponse,
    summary="Revoke the active device binding for a patient (DELETE alias)",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def revoke_patient_device(
    patient_id: UUID,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> DeviceRevokeResponse:
    _get_primary_caregiver_link(patient_id, caregiver, db)

    device = db.scalar(
        select(PatientDevice).where(
            PatientDevice.patient_id == patient_id,
            PatientDevice.status == "ACTIVE",
        )
    )
    if device is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No active device found for this patient",
        )

    device.status = "REVOKED"
    device.revoked_at = datetime.now(timezone.utc)
    db.commit()
    db.refresh(device)

    return DeviceRevokeResponse(
        device_identifier=device.device_identifier,
        status=device.status,
        revoked_at=device.revoked_at,
    )
