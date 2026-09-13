from __future__ import annotations

import argparse
from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal
from uuid import UUID

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.caregiver import Caregiver
from app.models.doctor import Doctor
from app.models.doctor_patient import DoctorPatient
from app.models.game import Game
from app.models.game_result import GameResult
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.models.performance_metric import PerformanceMetric
from app.models.reminder import Reminder
from app.models.user import User
from app.security import hash_password

DEMO_CAREGIVER_EMAIL = "demo.caregiver@cogni-care.example"
DEMO_SECONDARY_CAREGIVER_EMAIL = "demo.secondary@cogni-care.example"
DEMO_DOCTOR_EMAIL = "demo.doctor@cogni-care.example"
DEMO_PATIENT_EMAIL = "demo.patient@cogni-care.example"
DEMO_CAREGIVER_PASSWORD = "DemoCaregiverOnly-2026!"
DEMO_SECONDARY_CAREGIVER_PASSWORD = "DemoSecondaryOnly-2026!"
DEMO_DOCTOR_PASSWORD = "DemoDoctorOnly-2026!"
DEMO_PATIENT_PASSWORD = "DemoPatientOnly-2026!"

DEMO_CAREGIVER_ID = UUID("d0000000-0000-0000-0000-000000000001")
DEMO_PATIENT_ID = UUID("d0000000-0000-0000-0000-000000000002")
DEMO_LINK_ID = UUID("d0000000-0000-0000-0000-000000000003")
DEMO_SECONDARY_CAREGIVER_ID = UUID("d0000000-0000-0000-0000-000000000004")
DEMO_DOCTOR_ID = UUID("d0000000-0000-0000-0000-000000000005")
DEMO_DOCTOR_LINK_ID = UUID("d0000000-0000-0000-0000-000000000006")

GAME_DEFINITIONS = (
    ("DAILY_RECALL", "Daily Recall & Sequence Recall", "MEMORY"),
    ("FAMILY_IDENTIFICATION", "Family Identification by Pictures", "MEMORY"),
    ("ORIENTATION", "Orientation Game", "MEMORY"),
    ("OBJECT_IDENTIFICATION", "Object Identification", "CONCENTRATION_ATTENTION"),
    ("OBJECT_MATCHING", "Object Matching", "CONCENTRATION_ATTENTION"),
)

DEMO_ACCOUNTS = (
    {
        "id": UUID("d0000000-0000-0000-0000-000000000011"),
        "email": DEMO_CAREGIVER_EMAIL,
        "password": DEMO_CAREGIVER_PASSWORD,
        "name": "Ananya Sharma (Demo)",
        "role": "CAREGIVER",
    },
    {
        "id": UUID("d0000000-0000-0000-0000-000000000012"),
        "email": DEMO_SECONDARY_CAREGIVER_EMAIL,
        "password": DEMO_SECONDARY_CAREGIVER_PASSWORD,
        "name": "Ravi Sharma (Demo)",
        "role": "CAREGIVER",
    },
    {
        "id": UUID("d0000000-0000-0000-0000-000000000013"),
        "email": "demo.caregiver.northeast@cogni-care.example",
        "password": "DemoNortheastCaregiver-2026!",
        "name": "Maya Das (Demo)",
        "role": "CAREGIVER",
    },
    {
        "id": UUID("d0000000-0000-0000-0000-000000000014"),
        "email": "demo.caregiver.community@cogni-care.example",
        "password": "DemoCommunityCaregiver-2026!",
        "name": "Lalrinpuii Sailo (Demo)",
        "role": "CAREGIVER",
    },
    {
        "id": UUID("d0000000-0000-0000-0000-000000000015"),
        "email": DEMO_DOCTOR_EMAIL,
        "password": DEMO_DOCTOR_PASSWORD,
        "name": "Dr. Ananya Mehta (Demo)",
        "role": "DOCTOR",
    },
    {
        "id": UUID("d0000000-0000-0000-0000-000000000016"),
        "email": "demo.doctor.northeast@cogni-care.example",
        "password": "DemoNortheastDoctor-2026!",
        "name": "Dr. Tashi Norbu (Demo)",
        "role": "DOCTOR",
    },
    {
        "id": UUID("d0000000-0000-0000-0000-000000000021"),
        "email": DEMO_PATIENT_EMAIL,
        "password": DEMO_PATIENT_PASSWORD,
        "name": "Meera Sharma (Demo)",
        "role": "PATIENT",
    },
    {
        "id": UUID("d0000000-0000-0000-0000-000000000022"),
        "email": "demo.patient.assam@cogni-care.example",
        "password": "DemoAssamPatient-2026!",
        "name": "Nabanita Das (Demo)",
        "role": "PATIENT",
    },
    {
        "id": UUID("d0000000-0000-0000-0000-000000000023"),
        "email": "demo.patient.sikkim@cogni-care.example",
        "password": "DemoSikkimPatient-2026!",
        "name": "Pema Bhutia (Demo)",
        "role": "PATIENT",
    },
    {
        "id": UUID("d0000000-0000-0000-0000-000000000024"),
        "email": "demo.patient.mizoram@cogni-care.example",
        "password": "DemoMizoramPatient-2026!",
        "name": "Lalhmingliani Sailo (Demo)",
        "role": "PATIENT",
    },
)

PROFILE_DATA = {
    "demo.patient@cogni-care.example": (date(1948, 5, 12), "FEMALE", "English", "Asia/Kolkata"),
    "demo.patient.assam@cogni-care.example": (date(1955, 9, 3), "FEMALE", "Assamese", "Asia/Kolkata"),
    "demo.patient.sikkim@cogni-care.example": (date(1946, 2, 21), "MALE", "Nepali", "Asia/Kolkata"),
    "demo.patient.mizoram@cogni-care.example": (date(1951, 11, 8), "FEMALE", "Mizo", "Asia/Kolkata"),
}

PATIENT_IDS = {
    email: UUID(f"d0000000-0000-0000-0000-0000000000{number}")
    for email, number in (
        (DEMO_PATIENT_EMAIL, "02"),
        ("demo.patient.assam@cogni-care.example", "25"),
        ("demo.patient.sikkim@cogni-care.example", "26"),
        ("demo.patient.mizoram@cogni-care.example", "27"),
    )
}
CAREGIVER_IDS = {
    DEMO_CAREGIVER_EMAIL: DEMO_CAREGIVER_ID,
    DEMO_SECONDARY_CAREGIVER_EMAIL: DEMO_SECONDARY_CAREGIVER_ID,
    "demo.caregiver.northeast@cogni-care.example": UUID("d0000000-0000-0000-0000-000000000007"),
    "demo.caregiver.community@cogni-care.example": UUID("d0000000-0000-0000-0000-000000000008"),
}
DOCTOR_IDS = {
    DEMO_DOCTOR_EMAIL: DEMO_DOCTOR_ID,
    "demo.doctor.northeast@cogni-care.example": UUID("d0000000-0000-0000-0000-000000000009"),
}

CAREGIVER_ASSIGNMENTS = (
    (DEMO_PATIENT_EMAIL, DEMO_CAREGIVER_EMAIL, True),
    (DEMO_PATIENT_EMAIL, DEMO_SECONDARY_CAREGIVER_EMAIL, False),
    ("demo.patient.assam@cogni-care.example", DEMO_CAREGIVER_EMAIL, True),
    ("demo.patient.assam@cogni-care.example", "demo.caregiver.community@cogni-care.example", False),
    ("demo.patient.sikkim@cogni-care.example", "demo.caregiver.northeast@cogni-care.example", True),
    ("demo.patient.sikkim@cogni-care.example", DEMO_SECONDARY_CAREGIVER_EMAIL, False),
    ("demo.patient.mizoram@cogni-care.example", "demo.caregiver.community@cogni-care.example", True),
    ("demo.patient.mizoram@cogni-care.example", "demo.caregiver.northeast@cogni-care.example", False),
)

DOCTOR_ASSIGNMENTS = (
    (DEMO_DOCTOR_EMAIL, DEMO_PATIENT_EMAIL),
    (DEMO_DOCTOR_EMAIL, "demo.patient.assam@cogni-care.example"),
    (DEMO_DOCTOR_EMAIL, "demo.patient.mizoram@cogni-care.example"),
    ("demo.doctor.northeast@cogni-care.example", "demo.patient.assam@cogni-care.example"),
    ("demo.doctor.northeast@cogni-care.example", "demo.patient.sikkim@cogni-care.example"),
    ("demo.doctor.northeast@cogni-care.example", "demo.patient.mizoram@cogni-care.example"),
)


def _utc_datetime(day: date, hour: int = 10) -> datetime:
    return datetime.combine(day, time(hour), tzinfo=timezone.utc)


def _delete_existing_demo_data(db: Session) -> None:
    emails = [account["email"] for account in DEMO_ACCOUNTS]
    user_ids = list(db.scalars(select(User.id).where(User.email.in_(emails))))
    patient_ids = list(db.scalars(select(Patient.id).where(Patient.user_id.in_(user_ids))))
    caregiver_ids = list(db.scalars(select(Caregiver.id).where(Caregiver.user_id.in_(user_ids))))
    doctor_ids = list(db.scalars(select(Doctor.id).where(Doctor.user_id.in_(user_ids))))

    if patient_ids:
        session_ids = select(GameSession.id).where(GameSession.patient_id.in_(patient_ids))
        db.execute(delete(GameResult).where(GameResult.session_id.in_(session_ids)))
        db.execute(delete(GameSession).where(GameSession.patient_id.in_(patient_ids)))
        db.execute(delete(PerformanceMetric).where(PerformanceMetric.patient_id.in_(patient_ids)))
        db.execute(delete(Reminder).where(Reminder.patient_id.in_(patient_ids)))
        db.execute(delete(DoctorPatient).where(DoctorPatient.patient_id.in_(patient_ids)))
        db.execute(delete(PatientCaregiver).where(PatientCaregiver.patient_id.in_(patient_ids)))
        db.execute(delete(Patient).where(Patient.id.in_(patient_ids)))
    if caregiver_ids:
        db.execute(delete(PatientCaregiver).where(PatientCaregiver.caregiver_id.in_(caregiver_ids)))
        db.execute(delete(Caregiver).where(Caregiver.id.in_(caregiver_ids)))
    if doctor_ids:
        db.execute(delete(DoctorPatient).where(DoctorPatient.doctor_id.in_(doctor_ids)))
        db.execute(delete(Doctor).where(Doctor.id.in_(doctor_ids)))
    if user_ids:
        db.execute(delete(User).where(User.id.in_(user_ids)))
    db.flush()


def _seed_games(db: Session) -> dict[str, Game]:
    games: dict[str, Game] = {}
    for code, name, category in GAME_DEFINITIONS:
        game = db.scalar(select(Game).where(Game.code == code))
        if game is None:
            game = Game(code=code, name=name, category=category, is_active=True)
            db.add(game)
        else:
            game.name = name
            game.category = category
            game.is_active = True
        games[code] = game
    db.flush()
    return games


def _seed_accounts(db: Session) -> tuple[dict[str, User], dict[str, Patient], dict[str, Caregiver], dict[str, Doctor]]:
    users: dict[str, User] = {}
    for account in DEMO_ACCOUNTS:
        user = User(
            id=account["id"],
            email=account["email"],
            password_hash=hash_password(account["password"]),
            display_name=account["name"],
            role=account["role"],
            is_active=True,
        )
        db.add(user)
        users[account["email"]] = user
    db.flush()

    caregivers = {
        email: Caregiver(
            id=CAREGIVER_IDS[email],
            user_id=users[email].id,
            caregiver_type="FAMILY" if email in (DEMO_CAREGIVER_EMAIL, "demo.caregiver.community@cogni-care.example") else "PROFESSIONAL_CAREGIVER",
            phone=f"+91-90000000{index:02d}",
        )
        for index, email in enumerate(CAREGIVER_IDS, start=1)
    }
    doctors = {
        email: Doctor(id=DOCTOR_IDS[email], user_id=users[email].id)
        for email in DOCTOR_IDS
    }
    patients = {
        email: Patient(
            id=PATIENT_IDS[email],
            user_id=users[email].id,
            date_of_birth=PROFILE_DATA[email][0],
            gender=PROFILE_DATA[email][1],
            preferred_language=PROFILE_DATA[email][2],
            timezone=PROFILE_DATA[email][3],
        )
        for email in PROFILE_DATA
    }
    db.add_all([*caregivers.values(), *doctors.values(), *patients.values()])
    db.flush()
    return users, patients, caregivers, doctors


def _seed_relationships(
    db: Session,
    patients: dict[str, Patient],
    caregivers: dict[str, Caregiver],
    doctors: dict[str, Doctor],
) -> None:
    for patient_email, caregiver_email, is_primary in CAREGIVER_ASSIGNMENTS:
        db.add(
            PatientCaregiver(
                patient_id=patients[patient_email].id,
                caregiver_id=caregivers[caregiver_email].id,
                is_primary=is_primary,
            )
        )
    for doctor_email, patient_email in DOCTOR_ASSIGNMENTS:
        db.add(
            DoctorPatient(
                doctor_id=doctors[doctor_email].id,
                patient_id=patients[patient_email].id,
            )
        )
    db.flush()


PATIENT_GAME_ROTATIONS = (
    ("DAILY_RECALL", "FAMILY_IDENTIFICATION", "ORIENTATION", "OBJECT_MATCHING", "OBJECT_IDENTIFICATION"),
    ("FAMILY_IDENTIFICATION", "DAILY_RECALL", "OBJECT_IDENTIFICATION", "ORIENTATION", "OBJECT_MATCHING"),
    ("ORIENTATION", "OBJECT_MATCHING", "DAILY_RECALL", "OBJECT_IDENTIFICATION", "FAMILY_IDENTIFICATION"),
    ("OBJECT_MATCHING", "ORIENTATION", "FAMILY_IDENTIFICATION", "DAILY_RECALL", "OBJECT_IDENTIFICATION"),
)
PATIENT_PATTERNS = (
    (True, 78, 0.55, 1100),
    (False, 82, 0.0, 1000),
    (False, 91, 0.0, 880),
    (False, 75, 0.0, 1250),
)


def _seed_activity(
    db: Session,
    games: dict[str, Game],
    patients: dict[str, Patient],
    today: date,
) -> None:
    for patient_index, (patient_email, patient) in enumerate(patients.items()):
        improving, starting_accuracy, trend, response_base = PATIENT_PATTERNS[patient_index]
        rotation = PATIENT_GAME_ROTATIONS[patient_index]
        session_count = 20 + patient_index
        for index in range(session_count):
            if patient_index == 3:
                age_days = 115 - index * 4
            else:
                age_days = session_count * 3 - index * 3
            started_at = _utc_datetime(today - timedelta(days=age_days), 8 + index % 5)
            game_code = rotation[index % len(rotation)]
            difficulty = min(3, 1 + index // 8)
            game_bias = 4 if patient_index == 2 and game_code in rotation[:2] else 0
            fluctuation = ((index * 7 + patient_index * 11) % 13) - 6
            accuracy = starting_accuracy + (index * trend if improving else 0) + game_bias + fluctuation
            accuracy = max(55, min(98, accuracy))
            total_questions = 12 + (index % 3) * 4
            correct_answers = round(total_questions * accuracy / 100)
            actual_accuracy = round(correct_answers / total_questions * 100, 2)
            response_time = max(520, response_base - (index * 14 if improving else 0) + (index % 4) * 35)
            score = round(actual_accuracy * (0.82 + difficulty * 0.04), 2)
            mistakes = total_questions - correct_answers
            session = GameSession(
                patient_id=patient.id,
                game_id=games[game_code].id,
                difficulty_level=difficulty,
                started_at=started_at,
                completed_at=started_at + timedelta(minutes=6 + index % 5),
                status="COMPLETED",
            )
            db.add(session)
            db.flush()
            db.add(
                GameResult(
                    session_id=session.id,
                    score=Decimal(str(score)),
                    accuracy=Decimal(str(actual_accuracy)),
                    correct_answers=correct_answers,
                    total_questions=total_questions,
                    response_time_ms=response_time,
                    mistakes=mistakes,
                )
            )

        metric_age = 45 if patient_index == 3 else 0
        for index in range(10):
            if patient_index == 0:
                memory = 68 + index * 2.2
                attention = 64 + index * 2.5
            elif patient_index == 1:
                memory = 70 + ((index * 13) % 24)
                attention = 66 + ((index * 17) % 28)
            elif patient_index == 2:
                memory = 90 + (index % 4)
                attention = 68 + index * 1.4
            else:
                memory = 72 + index * 0.5
                attention = 70 + index * 0.4
            db.add(
                PerformanceMetric(
                    patient_id=patient.id,
                    metric_date=today - timedelta(days=metric_age + index),
                    memory_score=Decimal(str(round(min(memory, 98), 2))),
                    attention_score=Decimal(str(round(min(attention, 98), 2))),
                    average_accuracy=Decimal(str(round((memory + attention) / 2, 2))),
                    average_response_time_ms=max(600, response_base - index * 15),
                    games_completed=2,
                    total_sessions=2,
                )
            )


def _seed_reminders(db: Session, patients: dict[str, Patient], today: date) -> None:
    reminder_sets = (
        (
            ("Complete today's memory game", "GAME", 1, True, "FREQ=DAILY"),
            ("Morning walk with family", "ACTIVITY", 2, True, "FREQ=WEEKLY"),
            ("Care review appointment", "APPOINTMENT", 7, True, None),
        ),
        (
            ("Practice family picture recall", "GAME", 1, True, None),
            ("Medication check-in", "MEDICATION", 3, True, "FREQ=DAILY"),
            ("Previous orientation reminder", "ACTIVITY", -4, False, None),
        ),
        (
            ("Object matching practice", "GAME", 2, True, "FREQ=DAILY"),
            ("Community activity", "ACTIVITY", 5, True, None),
            ("Doctor follow-up", "APPOINTMENT", 12, True, None),
        ),
        (
            ("Monthly care review", "APPOINTMENT", 18, True, None),
            ("Previous game reminder", "GAME", -8, False, None),
        ),
    )
    for patient_index, (patient_email, patient) in enumerate(patients.items()):
        for title, reminder_type, offset, active, recurrence_rule in reminder_sets[patient_index]:
            db.add(
                Reminder(
                    patient_id=patient.id,
                    title=title,
                    description="Development dataset reminder for caregiver workflow testing.",
                    reminder_type=reminder_type,
                    scheduled_at=_utc_datetime(today + timedelta(days=offset), 9),
                    is_recurring=recurrence_rule is not None,
                    recurrence_rule=recurrence_rule,
                    is_active=active,
                )
            )


def seed_demo_data() -> tuple[UUID, UUID]:
    db = SessionLocal()
    try:
        _delete_existing_demo_data(db)
        games = _seed_games(db)
        _, patients, caregivers, doctors = _seed_accounts(db)
        _seed_relationships(db, patients, caregivers, doctors)
        today = datetime.now(timezone.utc).date()
        _seed_activity(db, games, patients, today)
        _seed_reminders(db, patients, today)
        db.commit()
        return caregivers[DEMO_CAREGIVER_EMAIL].id, patients[DEMO_PATIENT_EMAIL].id
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed repeatable Cogni-Care development data.")
    parser.parse_args()
    caregiver_id, patient_id = seed_demo_data()
    print(f"Seeded development dataset: 10 users, primary caregiver {caregiver_id}, patient {patient_id}")


if __name__ == "__main__":
    main()
