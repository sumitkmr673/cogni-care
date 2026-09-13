import os
import unittest
from datetime import datetime, timezone
from uuid import uuid4

os.environ.setdefault("JWT_SECRET_KEY", "test-only-secret")

from fastapi.testclient import TestClient
from sqlalchemy import delete, select

from app.db.session import SessionLocal
from app.main import app
from app.models.caregiver import Caregiver
from app.models.game import Game
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.user import User
from app.scripts.seed_demo import (
    DEMO_CAREGIVER_EMAIL,
    DEMO_CAREGIVER_PASSWORD,
    DEMO_DOCTOR_EMAIL,
    DEMO_DOCTOR_PASSWORD,
    DEMO_PATIENT_EMAIL,
    DEMO_PATIENT_PASSWORD,
    DEMO_SECONDARY_CAREGIVER_EMAIL,
    DEMO_SECONDARY_CAREGIVER_PASSWORD,
    seed_demo_data,
)
from app.security import hash_password


PATIENT_ID = "d0000000-0000-0000-0000-000000000002"


class RealRoleAuthorizationTests(unittest.TestCase):
    def setUp(self):
        seed_demo_data()
        self.client = TestClient(app)
        self.db = SessionLocal()
        self._temporary_user_ids = []
        self._temporary_patient_ids = []
        self.patient_id = PATIENT_ID
        self.caregiver_id = self._user_caregiver_id(DEMO_CAREGIVER_EMAIL)
        self.secondary_id = self._user_caregiver_id(DEMO_SECONDARY_CAREGIVER_EMAIL)

    def tearDown(self):
        self.db.execute(
            delete(GameSession).where(GameSession.patient_id.in_(self._temporary_patient_ids))
        )
        self.db.execute(delete(Patient).where(Patient.id.in_(self._temporary_patient_ids)))
        self.db.execute(delete(Caregiver).where(Caregiver.user_id.in_(self._temporary_user_ids)))
        self.db.execute(delete(User).where(User.id.in_(self._temporary_user_ids)))
        self.db.commit()
        self.db.close()
        self.client.close()

    def _user_caregiver_id(self, email):
        user = self.db.scalar(select(User).where(User.email == email))
        return str(self.db.scalar(select(Caregiver.id).where(Caregiver.user_id == user.id)))

    def _token(self, email, password):
        response = self.client.post("/auth/login", json={"email": email, "password": password})
        self.assertEqual(response.status_code, 200)
        return response.json()["access_token"]

    def _headers(self, email, password):
        return {"Authorization": f"Bearer {self._token(email, password)}"}

    def _demo_headers(self):
        return self._headers(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)

    def test_doctor_assigned_patient_is_allowed(self):
        headers = self._headers(DEMO_DOCTOR_EMAIL, DEMO_DOCTOR_PASSWORD)
        self.assertEqual(self.client.get(f"/patients/{self.patient_id}/dashboard", headers=headers).status_code, 200)
        self.assertEqual(self.client.get(f"/patients/{self.patient_id}/performance", headers=headers).status_code, 200)
        self.assertEqual(self.client.get(f"/patients/{self.patient_id}/sessions", headers=headers).status_code, 200)

    def test_doctor_unassigned_patient_is_denied(self):
        headers = self._headers(DEMO_DOCTOR_EMAIL, DEMO_DOCTOR_PASSWORD)
        response = self.client.get(
            "/patients/00000000-0000-0000-0000-000000000099/dashboard",
            headers=headers,
        )
        self.assertEqual(response.status_code, 404)

    def test_primary_and_secondary_caregivers_can_access_assigned_patient(self):
        primary = self._demo_headers()
        secondary = self._headers(DEMO_SECONDARY_CAREGIVER_EMAIL, DEMO_SECONDARY_CAREGIVER_PASSWORD)
        self.assertEqual(self.client.get(f"/patients/{self.patient_id}/dashboard", headers=primary).status_code, 200)
        self.assertEqual(self.client.get(f"/patients/{self.patient_id}/dashboard", headers=secondary).status_code, 200)

    def test_unassigned_caregiver_is_denied(self):
        user = User(
            email=f"unassigned-{uuid4()}@example.com",
            password_hash=hash_password("secret"),
            display_name="Unassigned Caregiver",
            role="CAREGIVER",
            is_active=True,
        )
        caregiver = Caregiver(user=user, caregiver_type="FAMILY")
        self.db.add_all([user, caregiver])
        self.db.commit()
        self._temporary_user_ids.append(user.id)
        headers = self._headers(user.email, "secret")
        response = self.client.get(f"/patients/{self.patient_id}/dashboard", headers=headers)
        self.assertEqual(response.status_code, 404)

    def test_primary_can_view_and_manage_care_team(self):
        headers = self._demo_headers()
        response = self.client.get(f"/patients/{self.patient_id}/care-team", headers=headers)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.json()["members"]), 2)
        duplicate = self.client.post(
            f"/patients/{self.patient_id}/care-team",
            headers=headers,
            json={"caregiver_id": self.secondary_id, "is_primary": False},
        )
        self.assertEqual(duplicate.status_code, 409)

    def test_invalid_care_team_target_is_not_found(self):
        response = self.client.post(
            f"/patients/{self.patient_id}/care-team",
            headers=self._demo_headers(),
            json={"caregiver_id": str(uuid4()), "is_primary": False},
        )
        self.assertEqual(response.status_code, 404)

    def test_secondary_caregiver_management_is_forbidden(self):
        headers = self._headers(DEMO_SECONDARY_CAREGIVER_EMAIL, DEMO_SECONDARY_CAREGIVER_PASSWORD)
        response = self.client.post(
            f"/patients/{self.patient_id}/care-team",
            headers=headers,
            json={"caregiver_id": self.caregiver_id, "is_primary": False},
        )
        self.assertEqual(response.status_code, 403)

    def test_doctor_and_patient_care_team_access_is_forbidden(self):
        doctor = self._headers(DEMO_DOCTOR_EMAIL, DEMO_DOCTOR_PASSWORD)
        patient = self._headers(DEMO_PATIENT_EMAIL, DEMO_PATIENT_PASSWORD)
        for headers in (doctor, patient):
            self.assertEqual(
                self.client.get(f"/patients/{self.patient_id}/care-team", headers=headers).status_code,
                403,
            )

    def test_second_primary_is_rejected(self):
        response = self.client.post(
            f"/patients/{self.patient_id}/care-team",
            headers=self._demo_headers(),
            json={"caregiver_id": self.secondary_id, "is_primary": True},
        )
        self.assertEqual(response.status_code, 409)

    def test_primary_transfer_changes_exactly_one_primary(self):
        response = self.client.put(
            f"/patients/{self.patient_id}/care-team/{self.secondary_id}/primary",
            headers=self._demo_headers(),
        )
        self.assertEqual(response.status_code, 200)
        members = self.client.get(
            f"/patients/{self.patient_id}/care-team",
            headers=self._demo_headers(),
        ).json()["members"]
        self.assertEqual(sum(member["is_primary"] for member in members), 1)
        self.assertTrue(next(member["is_primary"] for member in members if member["caregiver_id"] == self.secondary_id))
        old_primary_management = self.client.post(
            f"/patients/{self.patient_id}/care-team",
            headers=self._demo_headers(),
            json={"caregiver_id": self.caregiver_id, "is_primary": False},
        )
        self.assertEqual(old_primary_management.status_code, 403)

    def test_primary_transfer_to_unassigned_caregiver_is_not_found(self):
        response = self.client.put(
            f"/patients/{self.patient_id}/care-team/{uuid4()}/primary",
            headers=self._demo_headers(),
        )
        self.assertEqual(response.status_code, 404)

    def test_secondary_cannot_transfer_primary(self):
        response = self.client.put(
            f"/patients/{self.patient_id}/care-team/{self.caregiver_id}/primary",
            headers=self._headers(DEMO_SECONDARY_CAREGIVER_EMAIL, DEMO_SECONDARY_CAREGIVER_PASSWORD),
        )
        self.assertEqual(response.status_code, 403)

    def test_patient_and_doctor_cannot_transfer_primary(self):
        for headers in (
            self._headers(DEMO_DOCTOR_EMAIL, DEMO_DOCTOR_PASSWORD),
            self._headers(DEMO_PATIENT_EMAIL, DEMO_PATIENT_PASSWORD),
        ):
            response = self.client.put(
                f"/patients/{self.patient_id}/care-team/{self.secondary_id}/primary",
                headers=headers,
            )
            self.assertEqual(response.status_code, 403)

    def test_primary_cannot_be_removed(self):
        response = self.client.delete(
            f"/patients/{self.patient_id}/care-team/{self.caregiver_id}",
            headers=self._demo_headers(),
        )
        self.assertEqual(response.status_code, 409)

    def test_primary_can_remove_secondary(self):
        response = self.client.delete(
            f"/patients/{self.patient_id}/care-team/{self.secondary_id}",
            headers=self._demo_headers(),
        )
        self.assertEqual(response.status_code, 204)

    def test_doctor_cannot_start_or_submit_gameplay(self):
        headers = self._headers(DEMO_DOCTOR_EMAIL, DEMO_DOCTOR_PASSWORD)
        game_id = str(self.db.scalar(select(Game.id).order_by(Game.id)))
        session_id = str(self.db.scalar(select(GameSession.id).where(GameSession.patient_id == self.patient_id)))
        self.assertEqual(
            self.client.post(f"/games/{game_id}/sessions", headers=headers, json={"difficulty_level": 1}).status_code,
            403,
        )
        self.assertEqual(
            self.client.post(
                f"/games/sessions/{session_id}/result",
                headers=headers,
                json={"score": 1, "accuracy": 1},
            ).status_code,
            403,
        )

    def test_patient_can_play_and_cannot_submit_another_patients_session(self):
        headers = self._headers(DEMO_PATIENT_EMAIL, DEMO_PATIENT_PASSWORD)
        game_id = str(self.db.scalar(select(Game.id).order_by(Game.id)))
        own = self.client.post(f"/games/{game_id}/sessions", headers=headers, json={"difficulty_level": 1})
        self.assertEqual(own.status_code, 201)
        own_result = self.client.post(
            f"/games/sessions/{own.json()['id']}/result",
            headers=headers,
            json={"score": 1, "accuracy": 100},
        )
        self.assertEqual(own_result.status_code, 201)

        other_user = User(
            email=f"other-patient-{uuid4()}@example.com",
            password_hash=hash_password("secret"),
            display_name="Other Patient",
            role="PATIENT",
            is_active=True,
        )
        other_patient = Patient(user=other_user)
        other_session = GameSession(
            patient=other_patient,
            game_id=uuid4(),
            difficulty_level=1,
            started_at=datetime.now(timezone.utc),
            status="STARTED",
        )
        game = self.db.scalar(select(Game).where(Game.id == game_id))
        other_session.game_id = game.id
        self.db.add_all([other_user, other_patient, other_session])
        self.db.commit()
        self._temporary_user_ids.append(other_user.id)
        self._temporary_patient_ids.append(other_patient.id)
        denied = self.client.post(
            f"/games/sessions/{other_session.id}/result",
            headers=headers,
            json={"score": 1, "accuracy": 100},
        )
        self.assertEqual(denied.status_code, 404)


if __name__ == "__main__":
    unittest.main()
