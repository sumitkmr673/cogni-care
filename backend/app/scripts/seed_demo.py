from __future__ import annotations

import argparse
from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal
from uuid import UUID

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.caregiver import Caregiver
from app.models.game import Game
from app.models.game_result import GameResult
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.models.performance_metric import PerformanceMetric
from app.models.reminder import Reminder
from app.models.user import User

DEMO_CAREGIVER_EMAIL = "demo.caregiver@cogni-care.example"
DEMO_PATIENT_EMAIL = "demo.patient@cogni-care.example"

DEMO_CAREGIVER_ID = UUID("d0000000-0000-0000-0000-000000000001")
DEMO_PATIENT_ID = UUID("d0000000-0000-0000-0000-000000000002")
DEMO_LINK_ID = UUID("d0000000-0000-0000-0000-000000000003")

GAME_DEFINITIONS = (
    ("DAILY_RECALL", "Daily Recall", "MEMORY"),
    ("FAMILY_IDENTIFICATION", "Family Identification", "MEMORY"),
    ("ORIENTATION", "Orientation", "MEMORY"),
    ("OBJECT_IDENTIFICATION", "Object Identification", "CONCENTRATION_ATTENTION"),
    ("OBJECT_MATCHING", "Object Matching", "CONCENTRATION_ATTENTION"),
)


def _utc_datetime(day: date, hour: int = 10) -> datetime:
    return datetime.combine(day, time(hour), tzinfo=timezone.utc)


def _delete_existing_demo_data(db: Session) -> None:
    caregiver_user = db.scalar(select(User).where(User.email == DEMO_CAREGIVER_EMAIL))
    patient_user = db.scalar(select(User).where(User.email == DEMO_PATIENT_EMAIL))

    caregiver_id = (
        db.scalar(select(Caregiver.id).where(Caregiver.user_id == caregiver_user.id))
        if caregiver_user
        else DEMO_CAREGIVER_ID
    )
    patient_id = (
        db.scalar(select(Patient.id).where(Patient.user_id == patient_user.id))
        if patient_user
        else DEMO_PATIENT_ID
    )

    session_ids = select(GameSession.id).where(GameSession.patient_id == patient_id)
    db.execute(delete(GameResult).where(GameResult.session_id.in_(session_ids)))
    db.execute(delete(GameSession).where(GameSession.patient_id == patient_id))
    db.execute(delete(PerformanceMetric).where(PerformanceMetric.patient_id == patient_id))
    db.execute(delete(Reminder).where(Reminder.patient_id == patient_id))
    db.execute(
        delete(PatientCaregiver).where(
            PatientCaregiver.patient_id == patient_id,
            PatientCaregiver.caregiver_id == caregiver_id,
        )
    )
    db.execute(delete(Patient).where(Patient.id == patient_id))
    db.execute(delete(Caregiver).where(Caregiver.id == caregiver_id))
    if patient_user:
        db.delete(patient_user)
    if caregiver_user:
        db.delete(caregiver_user)
    db.flush()


def _seed_games(db: Session) -> dict[str, Game]:
    games: dict[str, Game] = {}
    for code, name, category in GAME_DEFINITIONS:
        game = db.scalar(select(Game).where(Game.code == code))
        if game is None:
            game = Game(code=code, name=name, category=category, is_active=True)
            db.add(game)
        games[code] = game
    db.flush()
    return games


def seed_demo_data() -> tuple[UUID, UUID]:
    db = SessionLocal()
    try:
        _delete_existing_demo_data(db)
        games = _seed_games(db)

        caregiver_user = User(
            email=DEMO_CAREGIVER_EMAIL,
            display_name="Dr. Ananya Mehta (Demo)",
            role="CAREGIVER",
            is_active=True,
        )
        patient_user = User(
            email=DEMO_PATIENT_EMAIL,
            display_name="Meera Sharma (Demo)",
            role="PATIENT",
            is_active=True,
        )
        db.add_all([caregiver_user, patient_user])
        db.flush()

        caregiver = Caregiver(
            id=DEMO_CAREGIVER_ID,
            user_id=caregiver_user.id,
            caregiver_type="DOCTOR",
            phone="+91-9876543210",
        )
        patient = Patient(
            id=DEMO_PATIENT_ID,
            user_id=patient_user.id,
            preferred_language="English",
            timezone="Asia/Kolkata",
            gender="FEMALE",
        )
        db.add_all([caregiver, patient])
        db.flush()

        db.add(
            PatientCaregiver(
                id=DEMO_LINK_ID,
                patient_id=patient.id,
                caregiver_id=caregiver.id,
                is_primary=True,
            )
        )

        today = datetime.now(timezone.utc).date()
        session_specs = (
            ("DAILY_RECALL", 2, 88.5, 92.0, 11, 12, 980, 1),
            ("OBJECT_MATCHING", 3, 91.0, 94.0, 19, 20, 900, 1),
            ("ORIENTATION", 2, 87.5, 90.0, 18, 20, 1040, 2),
            ("FAMILY_IDENTIFICATION", 3, 92.0, 95.0, 19, 20, 870, 1),
            ("OBJECT_IDENTIFICATION", 2, 89.5, 93.0, 22, 24, 940, 1),
            ("DAILY_RECALL", 2, 84.0, 88.0, 11, 13, 1210, 2),
            ("OBJECT_MATCHING", 2, 81.0, 85.0, 17, 20, 1320, 3),
            ("ORIENTATION", 1, 79.5, 84.0, 16, 19, 1250, 2),
            ("FAMILY_IDENTIFICATION", 2, 86.0, 89.0, 17, 19, 1110, 2),
            ("OBJECT_IDENTIFICATION", 1, 76.5, 82.0, 9, 11, 1480, 2),
        )
        for offset, (code, difficulty, score, accuracy, correct, total, response_ms, mistakes) in enumerate(
            session_specs
        ):
            started_at = _utc_datetime(today - timedelta(days=offset), 9 + offset % 4)
            session = GameSession(
                patient_id=patient.id,
                game_id=games[code].id,
                difficulty_level=difficulty,
                started_at=started_at,
                completed_at=started_at + timedelta(minutes=8),
                status="COMPLETED",
            )
            db.add(session)
            db.flush()
            db.add(
                GameResult(
                    session_id=session.id,
                    score=Decimal(str(score)),
                    accuracy=Decimal(str(accuracy)),
                    correct_answers=correct,
                    total_questions=total,
                    response_time_ms=response_ms,
                    mistakes=mistakes,
                )
            )

        for offset in range(10):
            metric_day = today - timedelta(days=offset)
            db.add(
                PerformanceMetric(
                    patient_id=patient.id,
                    metric_date=metric_day,
                    memory_score=Decimal(str(88.0 - offset * 1.1)),
                    attention_score=Decimal(str(90.0 - offset * 1.0)),
                    average_accuracy=Decimal(str(92.0 - offset * 0.9)),
                    average_response_time_ms=950 + offset * 55,
                    games_completed=1,
                    total_sessions=1,
                )
            )

        db.add_all(
            [
                Reminder(
                    patient_id=patient.id,
                    title="Complete today's memory game",
                    description="Try one short cognitive activity.",
                    reminder_type="GAME",
                    scheduled_at=_utc_datetime(today + timedelta(days=1), 8),
                    is_active=True,
                ),
                Reminder(
                    patient_id=patient.id,
                    title="Morning walk",
                    description="A gentle 20-minute walk with support.",
                    reminder_type="ACTIVITY",
                    scheduled_at=_utc_datetime(today + timedelta(days=2), 6),
                    is_active=True,
                ),
                Reminder(
                    patient_id=patient.id,
                    title="Care review appointment",
                    description="Demo follow-up appointment.",
                    reminder_type="APPOINTMENT",
                    scheduled_at=_utc_datetime(today + timedelta(days=5), 11),
                    is_active=True,
                ),
            ]
        )
        db.commit()
        return caregiver.id, patient.id
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed repeatable Cogni-Care dashboard demo data.")
    parser.parse_args()
    caregiver_id, patient_id = seed_demo_data()
    print(f"Seeded demo caregiver: {caregiver_id}")
    print(f"Seeded demo patient: {patient_id}")


if __name__ == "__main__":
    main()
