import os
import unittest
from datetime import datetime, timedelta, timezone
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
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.models.reminder import Reminder
from app.models.user import User
from app.scripts.seed_demo import (
    DEMO_CAREGIVER_EMAIL,
    DEMO_CAREGIVER_PASSWORD,
    DEMO_SECONDARY_CAREGIVER_EMAIL,
    DEMO_SECONDARY_CAREGIVER_PASSWORD,
    seed_demo_data,
)

DEMO_PATIENT_ID = "d0000000-0000-0000-0000-000000000002"
OTHER_DEMO_PATIENT_ID = "d0000000-0000-0000-0000-000000000003"


class ReminderManagementTests(unittest.TestCase):
    def setUp(self):
        seed_demo_data()
        self.client = TestClient(app)
        self.db = SessionLocal()

        # Login Primary Caregiver
        primary_login = self.client.post(
            "/auth/login",
            json={"email": DEMO_CAREGIVER_EMAIL, "password": DEMO_CAREGIVER_PASSWORD},
        )
        self.assertEqual(primary_login.status_code, 200)
        self.primary_token = primary_login.json()["access_token"]

        # Login Secondary Caregiver
        secondary_login = self.client.post(
            "/auth/login",
            json={"email": DEMO_SECONDARY_CAREGIVER_EMAIL, "password": DEMO_SECONDARY_CAREGIVER_PASSWORD},
        )
        self.assertEqual(secondary_login.status_code, 200)
        self.secondary_token = secondary_login.json()["access_token"]

        # Register and Login an Unassigned Caregiver
        self.unassigned_email = f"unassigned.{uuid4().hex[:8]}@example.com"
        reg_res = self.client.post(
            "/auth/register",
            json={
                "email": self.unassigned_email,
                "password": "Password123!",
                "display_name": "Unassigned Caregiver",
                "caregiver_type": "FAMILY",
            },
        )
        self.assertEqual(reg_res.status_code, 201)
        self.unassigned_cg_public_id = reg_res.json()["public_id"]
        unassigned_login = self.client.post(
            "/auth/login",
            json={"email": self.unassigned_email, "password": "Password123!"},
        )
        self.assertEqual(unassigned_login.status_code, 200)
        self.unassigned_token = unassigned_login.json()["access_token"]

        # Resolve public IDs
        primary_me = self.client.get("/auth/me", headers=self._auth_header(self.primary_token)).json()
        secondary_me = self.client.get("/auth/me", headers=self._auth_header(self.secondary_token)).json()
        self.primary_public_id = primary_me["public_id"]
        self.secondary_public_id = secondary_me["public_id"]

    def tearDown(self):
        # Clean up unassigned user
        user = self.db.scalar(select(User).where(User.email == self.unassigned_email))
        if user:
            self.db.execute(delete(Caregiver).where(Caregiver.user_id == user.id))
            self.db.execute(delete(User).where(User.id == user.id))
            self.db.commit()
        self.db.close()

    def _auth_header(self, token: str) -> dict[str, str]:
        return {"Authorization": f"Bearer {token}"}

    def _sample_reminder_payload(self, title: str = "Test Reminder") -> dict:
        scheduled_time = (datetime.now(timezone.utc) + timedelta(days=2)).isoformat()
        return {
            "title": title,
            "description": "Test reminder description",
            "reminder_type": "GAME",
            "scheduled_at": scheduled_time,
            "is_recurring": False,
            "recurrence_rule": None,
            "is_active": True,
        }

    # A. Primary creates reminder -> succeeds, creator is primary caregiver
    def test_a_primary_creates_reminder(self):
        payload = self._sample_reminder_payload("Primary Created Reminder")
        response = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertEqual(data["title"], "Primary Created Reminder")
        self.assertEqual(data["created_by_caregiver_public_id"], self.primary_public_id)
        self.assertTrue(data["is_active"])

    # B. Secondary creates reminder -> succeeds, creator is secondary caregiver
    def test_b_secondary_creates_reminder(self):
        payload = self._sample_reminder_payload("Secondary Created Reminder")
        response = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(response.status_code, 201)
        data = response.json()
        self.assertEqual(data["title"], "Secondary Created Reminder")
        self.assertEqual(data["created_by_caregiver_public_id"], self.secondary_public_id)

    # C. Secondary can view own reminder -> succeeds
    def test_c_secondary_can_view_own_reminder(self):
        payload = self._sample_reminder_payload("Secondary Own Reminder")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.secondary_token),
        )
        reminder_id = create_res.json()["id"]

        # View via list
        list_res = self.client.get(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(list_res.status_code, 200)
        found = any(r["id"] == reminder_id for r in list_res.json())
        self.assertTrue(found)

        # View via single GET
        single_res = self.client.get(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(single_res.status_code, 200)
        self.assertEqual(single_res.json()["id"], reminder_id)

    # D. Secondary can view another caregiver's reminder -> succeeds
    def test_d_secondary_can_view_another_caregivers_reminder(self):
        payload = self._sample_reminder_payload("Primary Created for Patient")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        reminder_id = create_res.json()["id"]

        # Secondary views via list
        list_res = self.client.get(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(list_res.status_code, 200)
        found = any(r["id"] == reminder_id for r in list_res.json())
        self.assertTrue(found)

        # Secondary views via single GET
        single_res = self.client.get(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(single_res.status_code, 200)
        self.assertEqual(single_res.json()["id"], reminder_id)

    # E. Secondary can edit own reminder -> succeeds
    def test_e_secondary_can_edit_own_reminder(self):
        payload = self._sample_reminder_payload("Secondary Original")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.secondary_token),
        )
        reminder_id = create_res.json()["id"]

        edit_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            json={"title": "Secondary Updated Title", "description": "Updated notes"},
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(edit_res.status_code, 200)
        self.assertEqual(edit_res.json()["title"], "Secondary Updated Title")
        self.assertEqual(edit_res.json()["description"], "Updated notes")
        # Creator must remain original creator
        self.assertEqual(edit_res.json()["created_by_caregiver_public_id"], self.secondary_public_id)

    # F. Secondary cannot edit another caregiver's reminder -> 403 Forbidden
    def test_f_secondary_cannot_edit_another_caregivers_reminder(self):
        payload = self._sample_reminder_payload("Primary Reminder")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        reminder_id = create_res.json()["id"]

        edit_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            json={"title": "Hacked Title"},
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(edit_res.status_code, 403)

    # G. Secondary can delete own reminder -> succeeds
    def test_g_secondary_can_delete_own_reminder(self):
        payload = self._sample_reminder_payload("Secondary To Delete")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.secondary_token),
        )
        reminder_id = create_res.json()["id"]

        del_res = self.client.delete(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(del_res.status_code, 204)

        # Confirm deleted
        get_res = self.client.get(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(get_res.status_code, 404)

    # H. Secondary cannot delete another caregiver's reminder -> 403 Forbidden
    def test_h_secondary_cannot_delete_another_caregivers_reminder(self):
        payload = self._sample_reminder_payload("Primary To Protect")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        reminder_id = create_res.json()["id"]

        del_res = self.client.delete(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(del_res.status_code, 403)

    # I. Secondary can toggle own reminder -> succeeds
    def test_i_secondary_can_toggle_own_reminder(self):
        payload = self._sample_reminder_payload("Secondary Toggle")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.secondary_token),
        )
        reminder_id = create_res.json()["id"]
        self.assertTrue(create_res.json()["is_active"])

        # Deactivate
        toggle_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}/status",
            json={"is_active": False},
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(toggle_res.status_code, 200)
        self.assertFalse(toggle_res.json()["is_active"])

        # Reactivate
        toggle_res2 = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}/status",
            json={"is_active": True},
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(toggle_res2.status_code, 200)
        self.assertTrue(toggle_res2.json()["is_active"])

    # J. Secondary cannot toggle another caregiver's reminder -> 403 Forbidden
    def test_j_secondary_cannot_toggle_another_caregivers_reminder(self):
        payload = self._sample_reminder_payload("Primary Toggle Target")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        reminder_id = create_res.json()["id"]

        toggle_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}/status",
            json={"is_active": False},
            headers=self._auth_header(self.secondary_token),
        )
        self.assertEqual(toggle_res.status_code, 403)

    # K. Primary can edit another caregiver's reminder -> succeeds
    def test_k_primary_can_edit_another_caregivers_reminder(self):
        payload = self._sample_reminder_payload("Created by Secondary")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.secondary_token),
        )
        reminder_id = create_res.json()["id"]

        edit_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            json={"title": "Primary Modified This Title"},
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(edit_res.status_code, 200)
        self.assertEqual(edit_res.json()["title"], "Primary Modified This Title")
        # Creator is preserved even after primary edits
        self.assertEqual(edit_res.json()["created_by_caregiver_public_id"], self.secondary_public_id)

    # L. Primary can delete another caregiver's reminder -> succeeds
    def test_l_primary_can_delete_another_caregivers_reminder(self):
        payload = self._sample_reminder_payload("Created by Secondary To Delete")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.secondary_token),
        )
        reminder_id = create_res.json()["id"]

        del_res = self.client.delete(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(del_res.status_code, 204)

        # Confirm deleted
        get_res = self.client.get(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(get_res.status_code, 404)

    # M. Primary can toggle another caregiver's reminder -> succeeds
    def test_m_primary_can_toggle_another_caregivers_reminder(self):
        payload = self._sample_reminder_payload("Secondary Reminder For Toggle")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.secondary_token),
        )
        reminder_id = create_res.json()["id"]

        toggle_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}/status",
            json={"is_active": False},
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(toggle_res.status_code, 200)
        self.assertFalse(toggle_res.json()["is_active"])

    # N. Unassigned caregiver cannot access reminders -> 404 (Patient not found)
    def test_n_unassigned_caregiver_cannot_access_reminders(self):
        list_res = self.client.get(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            headers=self._auth_header(self.unassigned_token),
        )
        self.assertEqual(list_res.status_code, 404)

        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=self._sample_reminder_payload("Unauthorized Create"),
            headers=self._auth_header(self.unassigned_token),
        )
        self.assertEqual(create_res.status_code, 404)

    # O. Patient/reminder mismatch cannot be used to access another patient's reminder -> 404
    def test_o_patient_reminder_mismatch_returns_404(self):
        payload = self._sample_reminder_payload("Patient 1 Reminder")
        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        reminder_id = create_res.json()["id"]

        # Attempt to access reminder under OTHER_DEMO_PATIENT_ID
        get_res = self.client.get(
            f"/patients/{OTHER_DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(get_res.status_code, 404)

        patch_res = self.client.patch(
            f"/patients/{OTHER_DEMO_PATIENT_ID}/reminders/{reminder_id}",
            json={"title": "Cross Patient Update Attempt"},
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(patch_res.status_code, 404)

        del_res = self.client.delete(
            f"/patients/{OTHER_DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(del_res.status_code, 404)

    # P. Creator ID cannot be spoofed through request payload
    def test_p_creator_id_cannot_be_spoofed(self):
        payload = self._sample_reminder_payload("Spoof Creator Attempt")
        # Attempt to inject secondary's public_id or an arbitrary UUID as creator
        payload["created_by_caregiver_id"] = str(uuid4())
        payload["created_by_caregiver_public_id"] = self.secondary_public_id

        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(create_res.status_code, 201)
        # Backend must have derived creator from authenticated user (primary), ignoring injected payload
        self.assertEqual(create_res.json()["created_by_caregiver_public_id"], self.primary_public_id)

    # Q. Existing non-recurring reminder: PATCH {"is_recurring": true} must NOT result in recurring=true with recurrence_rule=null
    def test_q_non_recurring_patched_with_is_recurring_true_without_rule_is_rejected(self):
        payload = self._sample_reminder_payload("Non-recurring Reminder")
        payload["is_recurring"] = False
        payload["recurrence_rule"] = None

        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(create_res.status_code, 201)
        reminder_id = create_res.json()["id"]

        # Attempt to PATCH {"is_recurring": true} without recurrence_rule
        patch_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            json={"is_recurring": True},
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(patch_res.status_code, 422)

        # Confirm DB state is untouched: not recurring, rule remains null
        get_res = self.client.get(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(get_res.status_code, 200)
        self.assertFalse(get_res.json()["is_recurring"])
        self.assertIsNone(get_res.json()["recurrence_rule"])

    # R. Existing recurring reminder: PATCH {"is_recurring": false} -> resulting reminder must have recurrence_rule=null
    def test_r_recurring_patched_with_is_recurring_false_clears_recurrence_rule_to_null(self):
        payload = self._sample_reminder_payload("Recurring Reminder")
        payload["is_recurring"] = True
        payload["recurrence_rule"] = "FREQ=DAILY;INTERVAL=1"

        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(create_res.status_code, 201)
        reminder_id = create_res.json()["id"]
        self.assertTrue(create_res.json()["is_recurring"])
        self.assertEqual(create_res.json()["recurrence_rule"], "FREQ=DAILY;INTERVAL=1")

        # PATCH {"is_recurring": false}
        patch_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            json={"is_recurring": False},
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(patch_res.status_code, 200)
        updated = patch_res.json()
        self.assertFalse(updated["is_recurring"])
        self.assertIsNone(updated["recurrence_rule"])

        # Confirm DB state persisted with recurrence_rule=null
        get_res = self.client.get(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(get_res.status_code, 200)
        self.assertFalse(get_res.json()["is_recurring"])
        self.assertIsNone(get_res.json()["recurrence_rule"])

    # S. Existing recurring reminder: PATCH {"is_recurring": false, "recurrence_rule": "..."} is rejected
    def test_s_recurring_patched_with_is_recurring_false_and_rule_is_rejected(self):
        payload = self._sample_reminder_payload("Recurring For Invalid Patch")
        payload["is_recurring"] = True
        payload["recurrence_rule"] = "FREQ=DAILY;INTERVAL=1"

        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(create_res.status_code, 201)
        reminder_id = create_res.json()["id"]

        patch_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            json={"is_recurring": False, "recurrence_rule": "FREQ=WEEKLY"},
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(patch_res.status_code, 422)

    # T. Existing non-recurring reminder: PATCH {"is_recurring": true, "recurrence_rule": "..."} succeeds
    def test_t_non_recurring_patched_with_is_recurring_true_and_rule_succeeds(self):
        payload = self._sample_reminder_payload("Non-recurring To Recurring")
        payload["is_recurring"] = False
        payload["recurrence_rule"] = None

        create_res = self.client.post(
            f"/patients/{DEMO_PATIENT_ID}/reminders",
            json=payload,
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(create_res.status_code, 201)
        reminder_id = create_res.json()["id"]

        patch_res = self.client.patch(
            f"/patients/{DEMO_PATIENT_ID}/reminders/{reminder_id}",
            json={"is_recurring": True, "recurrence_rule": "FREQ=DAILY;INTERVAL=1"},
            headers=self._auth_header(self.primary_token),
        )
        self.assertEqual(patch_res.status_code, 200)
        updated = patch_res.json()
        self.assertTrue(updated["is_recurring"])
        self.assertEqual(updated["recurrence_rule"], "FREQ=DAILY;INTERVAL=1")


if __name__ == "__main__":
    unittest.main()
