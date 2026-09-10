from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.api.dependencies import get_current_user, get_db
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
    PatientProfile,
    PatientSummary,
    PatientsResponse,
    PerformanceHistoryResponse,
    PerformancePoint,
    RecentGameSession,
    ReminderItem,
    SessionResult,
)

router = APIRouter(tags=["caregiver dashboard"])

CAREGIVER_ACCESS_DESCRIPTION = (
    "Requires a Bearer access token for an authenticated caregiver. "
    "Caregiver identity is taken from the JWT, not from a client-supplied caregiver ID."
)


def get_current_caregiver(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Caregiver:
    if current_user.role != "CAREGIVER":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Caregiver access required",
        )
    caregiver = db.scalar(select(Caregiver).where(Caregiver.user_id == current_user.id))
    if caregiver is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Caregiver access required",
        )
    return caregiver


def _accessible_patient(
    db: Session,
    patient_id: UUID,
    caregiver_id: UUID,
) -> Patient:
    patient = db.scalar(
        select(Patient)
        .join(PatientCaregiver, PatientCaregiver.patient_id == Patient.id)
        .where(
            Patient.id == patient_id,
            PatientCaregiver.caregiver_id == caregiver_id,
        )
    )
    if patient is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found",
        )
    return patient


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
                display_name=display_name,
                preferred_language=patient.preferred_language,
                timezone=patient.timezone,
                profile_photo_ref=patient.profile_photo_ref,
            )
            for patient, display_name in rows
        ]
    )


@router.get(
    "/patients/{patient_id}/dashboard",
    response_model=DashboardResponse,
    summary="Get caregiver dashboard data for one patient",
    description=CAREGIVER_ACCESS_DESCRIPTION,
)
def get_patient_dashboard(
    patient_id: UUID,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> DashboardResponse:
    patient = _accessible_patient(db, patient_id, caregiver.id)
    display_name = db.scalar(select(User.display_name).where(User.id == patient.user_id))

    relationship_row = db.execute(
        select(PatientCaregiver, Caregiver, User.display_name)
        .join(Caregiver, Caregiver.id == PatientCaregiver.caregiver_id)
        .join(User, User.id == Caregiver.user_id)
        .where(
            PatientCaregiver.patient_id == patient_id,
            PatientCaregiver.caregiver_id == caregiver.id,
        )
    ).one_or_none()

    caregiver_relationship = None
    if relationship_row is not None:
        link, caregiver, caregiver_name = relationship_row
        caregiver_relationship = CaregiverRelationship(
            caregiver_id=caregiver.id,
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
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> PerformanceHistoryResponse:
    _accessible_patient(db, patient_id, caregiver.id)
    metrics = db.scalars(
        select(PerformanceMetric)
        .where(PerformanceMetric.patient_id == patient_id)
        .order_by(PerformanceMetric.metric_date.asc())
    ).all()

    return PerformanceHistoryResponse(
        patient_id=patient_id,
        metrics=[PerformancePoint.model_validate(metric) for metric in metrics],
    )
