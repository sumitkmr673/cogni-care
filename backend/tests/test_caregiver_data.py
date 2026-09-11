import unittest
from datetime import datetime, timezone
from uuid import uuid4

from fastapi import HTTPException
from pydantic import ValidationError

from app.api.dependencies import get_accessible_patient, get_current_caregiver
from app.models.caregiver import Caregiver
from app.models.patient import Patient
from app.models.user import User
from app.schemas.reminder import ReminderCreateRequest


class FakeSession:
    def __init__(self, result):
        self.result = result

    def scalar(self, statement):
        return self.result


class CaregiverDataTests(unittest.TestCase):
    def test_only_caregiver_users_resolve_to_caregiver_profiles(self):
        user = User(id=uuid4(), role="PATIENT", is_active=True)
        with self.assertRaises(HTTPException) as error:
            get_current_caregiver(user, FakeSession(None))
        self.assertEqual(error.exception.status_code, 403)

    def test_caregiver_profile_resolves_from_authenticated_user(self):
        user = User(id=uuid4(), role="CAREGIVER", is_active=True)
        caregiver = Caregiver(id=uuid4(), user_id=user.id, caregiver_type="DOCTOR")
        resolved = get_current_caregiver(user, FakeSession(caregiver))
        self.assertIs(resolved, caregiver)

    def test_patient_access_is_scoped_to_caregiver_relationship(self):
        caregiver = Caregiver(id=uuid4(), user_id=uuid4(), caregiver_type="DOCTOR")
        patient = Patient(id=uuid4(), user_id=uuid4(), preferred_language="en", timezone="UTC")
        self.assertIs(
            get_accessible_patient(patient.id, caregiver, FakeSession(patient)),
            patient,
        )

        with self.assertRaises(HTTPException) as error:
            get_accessible_patient(patient.id, caregiver, FakeSession(None))
        self.assertEqual(error.exception.status_code, 404)

    def test_reminder_recurrence_rules_are_validated(self):
        common = {
            "title": "Play a game",
            "reminder_type": "GAME",
            "scheduled_at": datetime.now(timezone.utc),
        }
        ReminderCreateRequest(**common)
        ReminderCreateRequest(
            **common,
            is_recurring=True,
            recurrence_rule="FREQ=DAILY",
        )
        for values in (
            {"is_recurring": True},
            {"recurrence_rule": "FREQ=DAILY"},
        ):
            with self.assertRaises(ValidationError):
                ReminderCreateRequest(**common, **values)
        with self.assertRaises(ValidationError):
            ReminderCreateRequest(**{**common, "reminder_type": "INVALID"})


if __name__ == "__main__":
    unittest.main()
