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
from app.models.user import User
from app.scripts.seed_demo import (
    DEMO_CAREGIVER_EMAIL,
    DEMO_CAREGIVER_PASSWORD,
    DEMO_SECONDARY_CAREGIVER_EMAIL,
    DEMO_SECONDARY_CAREGIVER_PASSWORD,
    seed_demo_data,
)

PUBLIC_ID_CG_REGEX = re.compile(r"^CG-[A-Z0-9]{8}$")
PUBLIC_ID_PT_REGEX = re.compile(r"^PT-[A-Z0-9]{8}$")
DEMO_PATIENT_ID = "d0000000-0000-0000-0000-000000000002"


class PatientWorkflowTests(unittest.TestCase):
    def setUp(self):
        seed_demo_data()
        self.client = TestClient(app)
        self.db = SessionLocal()
        self._temporary_user_ids = []
        self._temporary_patient_ids = []

    def tearDown(self):
        if self._temporary_patient_ids:
            self.db.execute(
                delete(GameSession).where(GameSession.patient_id.in_(self._temporary_patient_ids))
            )
            self.db.execute(
                delete(PatientCaregiver).where(PatientCaregiver.patient_id.in_(self._temporary_patient_ids))
            )
            self.db.execute(
                delete(Patient).where(Patient.id.in_(self._temporary_patient_ids))
            )
        if self._temporary_user_ids:
            self.db.execute(
                delete(PatientCaregiver).where(
                    PatientCaregiver.caregiver_id.in_(
                        select(Caregiver.id).where(Caregiver.user_id.in_(self._temporary_user_ids))
                    )
                )
            )
            self.db.execute(
                delete(Caregiver).where(Caregiver.user_id.in_(self._temporary_user_ids))
            )
            self.db.execute(
                delete(User).where(User.id.in_(self._temporary_user_ids))
            )
        self.db.commit()
        self.db.close()
        self.client.close()

    def _track_user_by_email(self, email: str):
        user = self.db.scalar(select(User).where(User.email == email))
        if user and user.id not in self._temporary_user_ids:
            self._temporary_user_ids.append(user.id)
        return user

    def _track_patient_by_id(self, patient_id):
        from uuid import UUID
        uid = UUID(str(patient_id))
        if uid not in self._temporary_patient_ids:
            self._temporary_patient_ids.append(uid)
            patient = self.db.scalar(select(Patient).where(Patient.id == uid))
            if patient and patient.user_id not in self._temporary_user_ids:
                self._temporary_user_ids.append(patient.user_id)

    def _token(self, email: str, password: str) -> str:
        response = self.client.post("/auth/login", json={"email": email, "password": password})
        self.assertEqual(response.status_code, 200, f"Login failed: {response.text}")
        return response.json()["access_token"]

    def _auth_header(self, token: str) -> dict[str, str]:
        return {"Authorization": f"Bearer {token}"}

    # 1. caregiver registration succeeds
    def test_01_caregiver_registration_succeeds(self):
        email = f"caregiver_{uuid4().hex[:8]}@example.com"
        payload = {
            "email": email,
            "password": "StrongPassword123!",
            "display_name": "Test Caregiver",
            "caregiver_type": "FAMILY",
            "phone": "+91-9876543210",
        }
        response = self.client.post("/auth/register", json=payload)
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertEqual(data["email"], email)
        self.assertEqual(data["display_name"], "Test Caregiver")
        self.assertEqual(data["role"], "CAREGIVER")
        self.assertTrue(PUBLIC_ID_CG_REGEX.match(data["public_id"]))
        self._track_user_by_email(email)

    # 2. duplicate caregiver email rejected
    def test_02_duplicate_caregiver_email_rejected(self):
        payload = {
            "email": DEMO_CAREGIVER_EMAIL,
            "password": "AnotherPassword123!",
            "display_name": "Duplicate Caregiver",
        }
        response = self.client.post("/auth/register", json=payload)
        self.assertEqual(response.status_code, 409)
        self.assertIn("already exists", response.json()["detail"])

    # 3. registered caregiver can login
    def test_03_registered_caregiver_can_login(self):
        email = f"login_{uuid4().hex[:8]}@example.com"
        password = "SecureLoginPassword123!"
        reg_response = self.client.post(
            "/auth/register",
            json={"email": email, "password": password, "display_name": "Login Test User"},
        )
        self.assertEqual(reg_response.status_code, 201)
        self._track_user_by_email(email)

        login_response = self.client.post(
            "/auth/login",
            json={"email": email, "password": password},
        )
        self.assertEqual(login_response.status_code, 200)
        self.assertIn("access_token", login_response.json())

    # 4. /auth/me returns caregiver public ID
    def test_04_auth_me_returns_caregiver_public_id(self):
        token = self._token(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        response = self.client.get("/auth/me", headers=self._auth_header(token))
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["email"], DEMO_CAREGIVER_EMAIL)
        self.assertIsNotNone(body.get("public_id"))
        self.assertEqual(body["public_id"], "CG-DEMO0001")
        self.assertEqual(body["caregiver_type"], "FAMILY")

    # 5. caregiver public ID has correct format
    def test_05_caregiver_public_id_format(self):
        email = f"format_{uuid4().hex[:8]}@example.com"
        reg_response = self.client.post(
            "/auth/register",
            json={"email": email, "password": "Password123!", "display_name": "Format Caregiver"},
        )
        self.assertEqual(reg_response.status_code, 201)
        self._track_user_by_email(email)
        public_id = reg_response.json()["public_id"]
        self.assertTrue(
            PUBLIC_ID_CG_REGEX.match(public_id),
            f"Public ID {public_id} does not match format CG-XXXXXXXX",
        )

    # 6. patient creation succeeds
    def test_06_patient_creation_succeeds(self):
        token = self._token(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        payload = {
            "display_name": "New Patient Test",
            "date_of_birth": "1950-01-15",
            "gender": "MALE",
            "preferred_language": "English",
            "timezone": "Asia/Kolkata",
        }
        response = self.client.post("/patients", json=payload, headers=self._auth_header(token))
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertEqual(data["display_name"], "New Patient Test")
        self.assertEqual(data["gender"], "MALE")
        self.assertEqual(data["preferred_language"], "English")
        self._track_patient_by_id(data["id"])

    # 7. created patient receives PT public ID
    def test_07_created_patient_receives_pt_public_id(self):
        token = self._token(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        response = self.client.post(
            "/patients",
            json={"display_name": "PT Format Test Patient"},
            headers=self._auth_header(token),
        )
        self.assertEqual(response.status_code, 201)
        data = response.json()
        public_id = data["public_id"]
        self.assertTrue(
            PUBLIC_ID_PT_REGEX.match(public_id),
            f"Public ID {public_id} does not match format PT-XXXXXXXX",
        )
        self._track_patient_by_id(data["id"])

    # 8. creating caregiver becomes primary
    def test_08_creating_caregiver_becomes_primary(self):
        email = f"creator_{uuid4().hex[:8]}@example.com"
        reg = self.client.post(
            "/auth/register",
            json={"email": email, "password": "Password123!", "display_name": "Creator CG"},
        )
        self.assertEqual(reg.status_code, 201)
        self._track_user_by_email(email)
        token = self._token(email, "Password123!")

        create_patient_res = self.client.post(
            "/patients",
            json={"display_name": "Creator Patient"},
            headers=self._auth_header(token),
        )
        self.assertEqual(create_patient_res.status_code, 201)
        patient_id = create_patient_res.json()["id"]
        self._track_patient_by_id(patient_id)

        # Check care team
        team_res = self.client.get(f"/patients/{patient_id}/care-team", headers=self._auth_header(token))
        self.assertEqual(team_res.status_code, 200)
        members = team_res.json()["members"]
        self.assertEqual(len(members), 1)
        self.assertTrue(members[0]["is_primary"])
        self.assertEqual(members[0]["public_id"], reg.json()["public_id"])

        # Check dashboard
        dash_res = self.client.get(f"/patients/{patient_id}/dashboard", headers=self._auth_header(token))
        self.assertEqual(dash_res.status_code, 200)
        rel = dash_res.json()["caregiver_relationship"]
        self.assertIsNotNone(rel)
        self.assertTrue(rel["is_primary"])
        self.assertEqual(rel["public_id"], reg.json()["public_id"])

    # 9. existing patient can be linked by PT public ID
    def test_09_existing_patient_can_be_linked_by_pt_public_id(self):
        email = f"linker_{uuid4().hex[:8]}@example.com"
        self.client.post(
            "/auth/register",
            json={"email": email, "password": "Password123!", "display_name": "Linker CG"},
        )
        self._track_user_by_email(email)
        token = self._token(email, "Password123!")

        # Link demo patient PT-DEMO0001
        link_res = self.client.post(
            "/patients/link",
            json={"public_id": "PT-DEMO0001"},
            headers=self._auth_header(token),
        )
        self.assertEqual(link_res.status_code, 200)
        self.assertEqual(link_res.json()["public_id"], "PT-DEMO0001")

        # Confirm new caregiver now sees patient in /patients
        list_res = self.client.get("/patients", headers=self._auth_header(token))
        self.assertEqual(list_res.status_code, 200)
        linked_ids = [p["public_id"] for p in list_res.json()["patients"]]
        self.assertIn("PT-DEMO0001", linked_ids)

    # 10. linking caregiver becomes secondary
    def test_10_linking_caregiver_becomes_secondary(self):
        email = f"secondary_{uuid4().hex[:8]}@example.com"
        self.client.post(
            "/auth/register",
            json={"email": email, "password": "Password123!", "display_name": "Secondary CG"},
        )
        self._track_user_by_email(email)
        token = self._token(email, "Password123!")

        # Link to PT-DEMO0004 (Mizoram patient)
        link_res = self.client.post(
            "/patients/link",
            json={"public_id": "PT-DEMO0004"},
            headers=self._auth_header(token),
        )
        self.assertEqual(link_res.status_code, 200)
        patient_id = link_res.json()["id"]

        dash_res = self.client.get(f"/patients/{patient_id}/dashboard", headers=self._auth_header(token))
        self.assertEqual(dash_res.status_code, 200)
        rel = dash_res.json()["caregiver_relationship"]
        self.assertIsNotNone(rel)
        self.assertFalse(rel["is_primary"], "Linking caregiver must be secondary (is_primary=False)")

    # 10b. linking unassigned patient (no primary) becomes primary
    def test_10b_linking_unassigned_patient_becomes_primary(self):
        user = User(
            display_name="Unassigned Patient",
            role="PATIENT",
            is_active=True,
        )
        self.db.add(user)
        self.db.flush()
        patient_pub_id = f"PT-{uuid4().hex[:8].upper()}"
        patient = Patient(
            user_id=user.id,
            public_id=patient_pub_id,
            preferred_language="English",
            timezone="Asia/Kolkata",
        )
        self.db.add(patient)
        self.db.commit()
        self._temporary_patient_ids.append(patient.id)
        self._temporary_user_ids.append(user.id)

        email = f"new_primary_{uuid4().hex[:8]}@example.com"
        self.client.post(
            "/auth/register",
            json={"email": email, "password": "Password123!", "display_name": "New Primary CG"},
        )
        self._track_user_by_email(email)
        token = self._token(email, "Password123!")

        link_res = self.client.post(
            "/patients/link",
            json={"public_id": patient_pub_id},
            headers=self._auth_header(token),
        )
        self.assertEqual(link_res.status_code, 200)

        dash_res = self.client.get(f"/patients/{patient.id}/dashboard", headers=self._auth_header(token))
        self.assertEqual(dash_res.status_code, 200)
        rel = dash_res.json()["caregiver_relationship"]
        self.assertIsNotNone(rel)
        self.assertTrue(rel["is_primary"], "Linking caregiver to unassigned patient must be primary (is_primary=True)")

    # 11. existing primary remains unchanged
    def test_11_existing_primary_remains_unchanged(self):
        email = f"checkprimary_{uuid4().hex[:8]}@example.com"
        self.client.post(
            "/auth/register",
            json={"email": email, "password": "Password123!", "display_name": "Other CG"},
        )
        self._track_user_by_email(email)
        token = self._token(email, "Password123!")

        # Link to PT-DEMO0001
        self.client.post(
            "/patients/link",
            json={"public_id": "PT-DEMO0001"},
            headers=self._auth_header(token),
        )

        # Check care team as primary caregiver
        primary_token = self._token(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        team_res = self.client.get(f"/patients/{DEMO_PATIENT_ID}/care-team", headers=self._auth_header(primary_token))
        self.assertEqual(team_res.status_code, 200)
        primaries = [m for m in team_res.json()["members"] if m["is_primary"]]
        self.assertEqual(len(primaries), 1)
        self.assertEqual(primaries[0]["public_id"], "CG-DEMO0001")

    # 12. duplicate patient-caregiver link rejected
    def test_12_duplicate_patient_caregiver_link_rejected(self):
        primary_token = self._token(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        response = self.client.post(
            "/patients/link",
            json={"public_id": "PT-DEMO0001"},
            headers=self._auth_header(primary_token),
        )
        self.assertEqual(response.status_code, 409)
        self.assertIn("already linked", response.json()["detail"])

    # 13. caregiver can add another caregiver using CG public ID
    def test_13_caregiver_can_add_another_caregiver_using_cg_public_id(self):
        primary_token = self._token(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)

        email = f"standalone_{uuid4().hex[:8]}@example.com"
        reg = self.client.post(
            "/auth/register",
            json={"email": email, "password": "Password123!", "display_name": "Standalone CG"},
        )
        self.assertEqual(reg.status_code, 201)
        self._track_user_by_email(email)
        cg_public_id = reg.json()["public_id"]

        response = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/care-team",
            json={"caregiver_public_id": cg_public_id},
            headers=self._auth_header(primary_token),
        )
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["public_id"], cg_public_id)

    # 14. added caregiver is secondary
    def test_14_added_caregiver_is_secondary(self):
        primary_token = self._token(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)
        email = f"added_sec_{uuid4().hex[:8]}@example.com"
        reg = self.client.post(
            "/auth/register",
            json={"email": email, "password": "Password123!", "display_name": "Added Sec CG"},
        )
        self.assertEqual(reg.status_code, 201)
        self._track_user_by_email(email)
        cg_public_id = reg.json()["public_id"]

        response = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/care-team",
            json={"caregiver_public_id": cg_public_id},
            headers=self._auth_header(primary_token),
        )
        self.assertEqual(response.status_code, 201)
        self.assertFalse(response.json()["is_primary"], "Added caregiver must default to secondary")

    # 15. unauthorized/unassigned caregiver cannot access patient data
    def test_15_unassigned_caregiver_cannot_access_patient_data(self):
        email = f"unassigned_{uuid4().hex[:8]}@example.com"
        self.client.post(
            "/auth/register",
            json={"email": email, "password": "Password123!", "display_name": "Unassigned CG"},
        )
        self._track_user_by_email(email)
        token = self._token(email, "Password123!")

        dash_res = self.client.get(f"/patients/{DEMO_PATIENT_ID}/dashboard", headers=self._auth_header(token))
        self.assertEqual(dash_res.status_code, 404)

        perf_res = self.client.get(f"/patients/{DEMO_PATIENT_ID}/performance", headers=self._auth_header(token))
        self.assertEqual(perf_res.status_code, 404)

        team_res = self.client.get(f"/patients/{DEMO_PATIENT_ID}/care-team", headers=self._auth_header(token))
        self.assertEqual(team_res.status_code, 404)

    # 16. primary-only mutations remain protected
    def test_16_primary_only_mutations_remain_protected(self):
        secondary_token = self._token(DEMO_SECONDARY_CAREGIVER_EMAIL, DEMO_SECONDARY_CAREGIVER_PASSWORD)

        # Secondary cannot add to care team
        add_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/care-team",
            json={"caregiver_public_id": "CG-DEMO0003"},
            headers=self._auth_header(secondary_token),
        )
        self.assertEqual(add_res.status_code, 403)

        # Secondary cannot transfer primary
        transfer_res = self.client.put(
            f"/patients/{DEMO_PATIENT_ID}/care-team/d0000000-0000-0000-0000-000000000004/primary",
            headers=self._auth_header(secondary_token),
        )
        self.assertEqual(transfer_res.status_code, 403)

    # 17. existing dashboard/performance APIs continue working
    def test_17_existing_dashboard_and_performance_continue_working(self):
        token = self._token(DEMO_CAREGIVER_EMAIL, DEMO_CAREGIVER_PASSWORD)

        # /patients list
        list_res = self.client.get("/patients", headers=self._auth_header(token))
        self.assertEqual(list_res.status_code, 200)
        patients = list_res.json()["patients"]
        self.assertGreaterEqual(len(patients), 1)
        self.assertTrue(all(p.get("public_id") is not None for p in patients))

        # /patients/{id}/dashboard
        dash_res = self.client.get(f"/patients/{DEMO_PATIENT_ID}/dashboard", headers=self._auth_header(token))
        self.assertEqual(dash_res.status_code, 200)
        dash_data = dash_res.json()
        self.assertEqual(dash_data["patient"]["public_id"], "PT-DEMO0001")
        self.assertEqual(dash_data["caregiver_relationship"]["public_id"], "CG-DEMO0001")
        self.assertIn("recent_sessions", dash_data)
        self.assertIn("latest_performance", dash_data)

        # /patients/{id}/performance
        perf_res = self.client.get(f"/patients/{DEMO_PATIENT_ID}/performance", headers=self._auth_header(token))
        self.assertEqual(perf_res.status_code, 200)
        self.assertIn("metrics", perf_res.json())

        # /patients/{id}/sessions
        sess_res = self.client.get(f"/patients/{DEMO_PATIENT_ID}/sessions", headers=self._auth_header(token))
        self.assertEqual(sess_res.status_code, 200)

        # /patients/{id}/trends
        trends_res = self.client.get(f"/patients/{DEMO_PATIENT_ID}/trends", headers=self._auth_header(token))
        self.assertEqual(trends_res.status_code, 200)

        # /patients/{id}/reminders
        rem_res = self.client.get(f"/patients/{DEMO_PATIENT_ID}/reminders", headers=self._auth_header(token))
        self.assertEqual(rem_res.status_code, 200)


if __name__ == "__main__":
    unittest.main()
