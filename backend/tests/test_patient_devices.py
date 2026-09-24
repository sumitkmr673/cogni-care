import os
import re
import unittest
from uuid import uuid4

os.environ.setdefault("JWT_SECRET_KEY", "test-only-secret")
os.environ.setdefault("POSTGRES_USER", "sih2026")
os.environ.setdefault("POSTGRES_PASSWORD", "sih2026_dev_snag")
os.environ.setdefault("POSTGRES_HOST", "localhost")
os.environ.setdefault("POSTGRES_PORT", "5432")
os.environ.setdefault("POSTGRES_DB", "sih2026")

from fastapi.testclient import TestClient
from sqlalchemy import delete, select

from app.db.session import SessionLocal
from app.main import app
from app.models.caregiver import Caregiver
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.models.patient_device import PatientDevice
from app.models.user import User
from app.scripts.seed_demo import (
    DEMO_CAREGIVER_EMAIL,
    DEMO_CAREGIVER_PASSWORD,
    DEMO_PATIENT_ID,
    DEMO_SECONDARY_CAREGIVER_EMAIL,
    DEMO_SECONDARY_CAREGIVER_PASSWORD,
    seed_demo_data,
)

PUBLIC_ID_PT_REGEX = re.compile(r"^PT-[A-Z0-9]{8}$")
DEVICE_ID_REGEX = re.compile(r"^DEV-[A-Z0-9]{12}$")


class PatientDeviceTests(unittest.TestCase):
    def setUp(self):
        seed_demo_data()
        self.client = TestClient(app)
        self.db = SessionLocal()
        self._cleanup_patient_ids = []
        self._cleanup_user_ids = []

        # Clean up any leftover patient_devices on demo patient
        self.db.execute(
            delete(PatientDevice).where(PatientDevice.patient_id == DEMO_PATIENT_ID)
        )
        self.db.commit()

    def tearDown(self):
        if self._cleanup_patient_ids:
            self.db.execute(
                delete(GameSession).where(GameSession.patient_id.in_(self._cleanup_patient_ids))
            )
            self.db.execute(
                delete(PatientDevice).where(PatientDevice.patient_id.in_(self._cleanup_patient_ids))
            )
            self.db.execute(
                delete(PatientCaregiver).where(PatientCaregiver.patient_id.in_(self._cleanup_patient_ids))
            )
            self.db.execute(
                delete(Patient).where(Patient.id.in_(self._cleanup_patient_ids))
            )
        if self._cleanup_user_ids:
            self.db.execute(
                delete(User).where(User.id.in_(self._cleanup_user_ids))
            )
        # Clear devices on demo patient
        self.db.execute(
            delete(PatientDevice).where(PatientDevice.patient_id == DEMO_PATIENT_ID)
        )
        self.db.commit()
        self.db.close()

    def _login_caregiver(self, email: str, password: str) -> str:
        response = self.client.post("/auth/login", json={"email": email, "password": password})
        self.assertEqual(response.status_code, 200)
        return response.json()["access_token"]

    def test_primary_caregiver_can_provision_patient_device(self):
        token = self._login_caregiver(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        response = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Living Room Tablet", "client_device_id": "client-uuid-1"},
        )
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertTrue(DEVICE_ID_REGEX.match(data["device_identifier"]))
        self.assertTrue(PUBLIC_ID_PT_REGEX.match(data["patient_public_id"]))
        self.assertTrue(len(data["device_key"]) >= 32)
        self.assertEqual(data["status"], "ACTIVE")
        self.assertEqual(data["patient_id"], str(DEMO_PATIENT_ID))

    def test_unauthorized_caregiver_cannot_provision_another_caregiver_patient(self):
        # Register an unrelated caregiver
        reg_response = self.client.post(
            "/auth/register",
            json={
                "display_name": "Unrelated Caregiver",
                "email": f"unrelated-{uuid4().hex[:8]}@example.com",
                "password": "Password123!",
                "caregiver_type": "FAMILY",
            },
        )
        self.assertEqual(reg_response.status_code, 201)
        unrelated_token = self.client.post(
            "/auth/login",
            json={"email": reg_response.json()["email"], "password": "Password123!"},
        ).json()["access_token"]

        response = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {unrelated_token}"},
            json={"device_name": "Intruder Device"},
        )
        self.assertEqual(response.status_code, 404)

    def test_secondary_caregiver_cannot_perform_primary_only_provisioning(self):
        token = self._login_caregiver(DEMO_SECONDARY_CAREGIVER_EMAIL, DEMO_SECONDARY_CAREGIVER_PASSWORD)
        response = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Secondary Attempt"},
        )
        self.assertEqual(response.status_code, 403)
        self.assertIn("primary caregiver", response.json()["detail"].lower())

    def test_patient_can_have_one_active_device_second_is_rejected(self):
        token = self._login_caregiver(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        res1 = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Device A", "client_device_id": "client-dev-a"},
        )
        self.assertEqual(res1.status_code, 201)
        dev_a_id = res1.json()["device_identifier"]

        # Attempt to provision Device B for the same patient -> must be rejected with 409
        res2 = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Device B", "client_device_id": "client-dev-b"},
        )
        self.assertEqual(res2.status_code, 409)
        self.assertIn("already has an active device", res2.json()["detail"].lower())

        # Verify Device A is still active in database
        active_dev = self.db.scalar(
            select(PatientDevice).where(
                PatientDevice.patient_id == DEMO_PATIENT_ID,
                PatientDevice.status == "ACTIVE",
            )
        )
        self.assertIsNotNone(active_dev)
        self.assertEqual(active_dev.device_identifier, dev_a_id)

    def test_device_cannot_simultaneously_belong_to_two_patients(self):
        token = self._login_caregiver(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)

        # Create second patient under primary caregiver
        p2_res = self.client.post(
            "/patients",
            headers={"Authorization": f"Bearer {token}"},
            json={"display_name": "Second Patient"},
        )
        self.assertEqual(p2_res.status_code, 201)
        p2_id = p2_res.json()["id"]
        self._cleanup_patient_ids.append(p2_id)

        shared_client_id = "physical-tablet-uuid-1234"

        # Provision for Demo Patient
        res1 = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Tablet", "client_device_id": shared_client_id},
        )
        self.assertEqual(res1.status_code, 201)

        # Attempt to provision same client_device_id for Second Patient
        res2 = self.client.post(
            f"/patients/{p2_id}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Tablet", "client_device_id": shared_client_id},
        )
        self.assertEqual(res2.status_code, 409)
        self.assertIn("already active for another patient", res2.json()["detail"].lower())

    def test_revoked_device_cannot_authenticate(self):
        token = self._login_caregiver(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        prov_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Soon Revoked"},
        )
        self.assertEqual(prov_res.status_code, 201)
        creds = prov_res.json()

        # Primary caregiver revokes device
        revoke_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices/revoke",
            headers={"Authorization": f"Bearer {token}"},
        )
        self.assertEqual(revoke_res.status_code, 200)
        self.assertEqual(revoke_res.json()["status"], "REVOKED")

        # Attempt to authenticate with revoked device
        auth_res = self.client.post(
            "/auth/device-login",
            json={
                "device_identifier": creds["device_identifier"],
                "device_key": creds["device_key"],
            },
        )
        self.assertEqual(auth_res.status_code, 401)
        self.assertIn("revoked", auth_res.json()["detail"].lower())

    def test_valid_active_device_can_authenticate_and_updates_last_seen(self):
        token = self._login_caregiver(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        prov_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Active Device"},
        )
        self.assertEqual(prov_res.status_code, 201)
        creds = prov_res.json()

        # Check last_seen_at is initially None
        db_device = self.db.scalar(
            select(PatientDevice).where(PatientDevice.device_identifier == creds["device_identifier"])
        )
        self.assertIsNone(db_device.last_seen_at)

        # Authenticate device
        auth_res = self.client.post(
            "/auth/device-login",
            json={
                "device_identifier": creds["device_identifier"],
                "device_key": creds["device_key"],
            },
        )
        self.assertEqual(auth_res.status_code, 200)
        self.assertIn("access_token", auth_res.json())

        # Verify last_seen_at was updated
        self.db.expire_all()
        db_device = self.db.scalar(
            select(PatientDevice).where(PatientDevice.device_identifier == creds["device_identifier"])
        )
        self.assertIsNotNone(db_device.last_seen_at)

    def test_patient_public_id_and_patient_id_in_auth_me(self):
        token = self._login_caregiver(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        prov_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Me Check Device"},
        )
        creds = prov_res.json()

        auth_res = self.client.post(
            "/auth/device-login",
            json={
                "device_identifier": creds["device_identifier"],
                "device_key": creds["device_key"],
            },
        )
        patient_token = auth_res.json()["access_token"]

        me_res = self.client.get("/auth/me", headers={"Authorization": f"Bearer {patient_token}"})
        self.assertEqual(me_res.status_code, 200)
        me_data = me_res.json()
        self.assertEqual(me_data["role"], "PATIENT")
        self.assertEqual(me_data["public_id"], creds["patient_public_id"])
        self.assertEqual(me_data["patient_id"], str(DEMO_PATIENT_ID))

    def test_gameplay_works_with_device_authenticated_patient_token(self):
        token = self._login_caregiver(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        prov_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/devices",
            headers={"Authorization": f"Bearer {token}"},
            json={"device_name": "Gameplay Device"},
        )
        creds = prov_res.json()

        auth_res = self.client.post(
            "/auth/device-login",
            json={
                "device_identifier": creds["device_identifier"],
                "device_key": creds["device_key"],
            },
        )
        patient_token = auth_res.json()["access_token"]

        # 1. List games
        games_res = self.client.get("/games", headers={"Authorization": f"Bearer {patient_token}"})
        self.assertEqual(games_res.status_code, 200)
        games = games_res.json()["games"]
        self.assertTrue(len(games) > 0)

        # 2. Start game session
        start_res = self.client.post(
            f"/games/{games[0]['id']}/sessions",
            headers={"Authorization": f"Bearer {patient_token}"},
            json={"difficulty_level": 1},
        )
        self.assertEqual(start_res.status_code, 201)
        session_id = start_res.json()["id"]
        self.assertIsNotNone(session_id)

    def test_existing_caregiver_authentication_still_works(self):
        res = self.client.post(
            "/auth/login",
            json={"email": DEMO_CAREGIVER_EMAIL, "password": DEMO_CAREGIVER_PASSWORD},
        )
        self.assertEqual(res.status_code, 200)
        self.assertIn("access_token", res.json())

        me_res = self.client.get("/auth/me", headers={"Authorization": f"Bearer {res.json()['access_token']}"})
        self.assertEqual(me_res.status_code, 200)
        self.assertEqual(me_res.json()["role"], "CAREGIVER")

    def test_patient_direct_registration_via_app(self):
        client_device_id = f"app-client-{uuid4().hex[:12]}"
        reg_res = self.client.post(
            "/auth/patient/register",
            json={
                "display_name": "App Onboarded Patient",
                "date_of_birth": "1948-05-15",
                "gender": "Woman",
                "preferred_language": "Hindi",
                "client_device_id": client_device_id,
                "device_name": "Grandmother Tablet",
            },
        )
        self.assertEqual(reg_res.status_code, 201)
        data = reg_res.json()
        self.assertEqual(data["display_name"], "App Onboarded Patient")
        self.assertTrue(PUBLIC_ID_PT_REGEX.match(data["patient_public_id"]))
        self.assertTrue(DEVICE_ID_REGEX.match(data["device_identifier"]))
        self.assertTrue(len(data["device_key"]) >= 32)
        self.assertIn("access_token", data["token"])

        self._cleanup_patient_ids.append(data["patient_id"])

        # Verify DOB and gender persisted in database
        self.db.expire_all()
        db_patient = self.db.scalar(select(Patient).where(Patient.id == data["patient_id"]))
        self.assertIsNotNone(db_patient)
        self.assertEqual(str(db_patient.date_of_birth), "1948-05-15")
        self.assertEqual(db_patient.gender, "Woman")

        # Immediately able to use access_token
        me_res = self.client.get(
            "/auth/me",
            headers={"Authorization": f"Bearer {data['token']['access_token']}"},
        )
        self.assertEqual(me_res.status_code, 200)
        self.assertEqual(me_res.json()["role"], "PATIENT")
        self.assertEqual(me_res.json()["public_id"], data["patient_public_id"])
        self.assertEqual(me_res.json()["patient_id"], data["patient_id"])

        # Subsequent device login with saved device_identifier and device_key
        login_res = self.client.post(
            "/auth/device-login",
            json={
                "device_identifier": data["device_identifier"],
                "device_key": data["device_key"],
            },
        )
        self.assertEqual(login_res.status_code, 200)
        self.assertIn("access_token", login_res.json())
