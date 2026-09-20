import os
import unittest
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal
from uuid import uuid4

os.environ.setdefault("JWT_SECRET_KEY", "test-only-secret")
os.environ.setdefault("POSTGRES_USER", "sih2026")
os.environ.setdefault("POSTGRES_PASSWORD", "sih2026_dev_snag")
os.environ.setdefault("POSTGRES_HOST", "localhost")
os.environ.setdefault("POSTGRES_PORT", "5432")
os.environ.setdefault("POSTGRES_DB", "sih2026")

from fastapi.testclient import TestClient
from sqlalchemy import select

from app.db.session import SessionLocal
from app.main import app
from app.models.caregiver import Caregiver
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.models.performance_metric import PerformanceMetric
from app.models.user import User
from app.scripts.seed_demo import seed_demo_data
from app.security import create_access_token, hash_password
from app.services.analysis import analyze_patient_performance


class RuleBasedAnalysisEngineTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        seed_demo_data()

    def setUp(self):
        self.client = TestClient(app)
        self.db = SessionLocal()

        self.suffix = uuid4().hex[:8]

        # Primary Caregiver
        self.caregiver_user = User(
            id=uuid4(),
            email=f"cg.primary.{self.suffix}@example.com",
            password_hash=hash_password("Password123!"),
            display_name=f"Primary CG {self.suffix}",
            role="CAREGIVER",
            is_active=True,
        )
        self.db.add(self.caregiver_user)
        self.db.flush()

        self.caregiver = Caregiver(
            id=uuid4(),
            user_id=self.caregiver_user.id,
            caregiver_type="FAMILY",
        )
        self.db.add(self.caregiver)
        self.db.flush()

        # Secondary / Doctor Caregiver
        self.doctor_user = User(
            id=uuid4(),
            email=f"cg.doctor.{self.suffix}@example.com",
            password_hash=hash_password("Password123!"),
            display_name=f"Dr. Smith {self.suffix}",
            role="CAREGIVER",
            is_active=True,
        )
        self.db.add(self.doctor_user)
        self.db.flush()

        self.doctor_caregiver = Caregiver(
            id=uuid4(),
            user_id=self.doctor_user.id,
            caregiver_type="DOCTOR",
        )
        self.db.add(self.doctor_caregiver)
        self.db.flush()

        # Unassigned Caregiver
        self.unassigned_user = User(
            id=uuid4(),
            email=f"cg.unassigned.{self.suffix}@example.com",
            password_hash=hash_password("Password123!"),
            display_name=f"Unassigned CG {self.suffix}",
            role="CAREGIVER",
            is_active=True,
        )
        self.db.add(self.unassigned_user)
        self.db.flush()

        self.unassigned_caregiver = Caregiver(
            id=uuid4(),
            user_id=self.unassigned_user.id,
            caregiver_type="FAMILY",
        )
        self.db.add(self.unassigned_caregiver)
        self.db.flush()

        # Patient
        self.patient_user = User(
            id=uuid4(),
            email=f"patient.{self.suffix}@example.com",
            password_hash=hash_password("Password123!"),
            display_name=f"Test Patient {self.suffix}",
            role="PATIENT",
            is_active=True,
        )
        self.db.add(self.patient_user)
        self.db.flush()

        self.patient = Patient(
            id=uuid4(),
            user_id=self.patient_user.id,
            timezone="Asia/Kolkata",
            preferred_language="English",
        )
        self.db.add(self.patient)
        self.db.flush()

        # Links
        self.link_primary = PatientCaregiver(
            patient_id=self.patient.id,
            caregiver_id=self.caregiver.id,
            is_primary=True,
        )
        self.link_doctor = PatientCaregiver(
            patient_id=self.patient.id,
            caregiver_id=self.doctor_caregiver.id,
            is_primary=False,
        )
        self.db.add_all([self.link_primary, self.link_doctor])
        self.db.commit()

        # Auth Tokens
        self.primary_token = create_access_token(self.caregiver_user.id)
        self.doctor_token = create_access_token(self.doctor_user.id)
        self.unassigned_token = create_access_token(self.unassigned_user.id)
        self.patient_token = create_access_token(self.patient_user.id)

    def tearDown(self):
        self.db.close()

    def _create_metric(
        self,
        days_ago: int,
        accuracy: float | None = 80.0,
        memory: float | None = 80.0,
        attention: float | None = 80.0,
        response_time_ms: int | None = 3000,
        games_completed: int = 3,
    ) -> PerformanceMetric:
        m = PerformanceMetric(
            id=uuid4(),
            patient_id=self.patient.id,
            metric_date=date.today() - timedelta(days=days_ago),
            average_accuracy=Decimal(str(accuracy)) if accuracy is not None else None,
            memory_score=Decimal(str(memory)) if memory is not None else None,
            attention_score=Decimal(str(attention)) if attention is not None else None,
            average_response_time_ms=response_time_ms,
            games_completed=games_completed,
            total_sessions=games_completed,
        )
        self.db.add(m)
        self.db.commit()
        return m

    def test_insufficient_data_zero_or_one_or_two_days(self):
        """When fewer than 3 active days exist, return INSUFFICIENT_DATA."""
        # 0 days
        resp = analyze_patient_performance(self.db, self.patient.id)
        self.assertEqual(resp.data_sufficiency, "INSUFFICIENT")
        self.assertEqual(resp.summary.primary_direction, "INSUFFICIENT_DATA")
        self.assertEqual(len(resp.observations), 1)
        self.assertEqual(resp.observations[0].category, "DATA_SUFFICIENCY")
        self.assertIn("At least 3 active days", resp.observations[0].message)

        # 2 days
        self._create_metric(days_ago=4, accuracy=80.0)
        self._create_metric(days_ago=3, accuracy=82.0)
        resp2 = analyze_patient_performance(self.db, self.patient.id)
        self.assertEqual(resp2.data_sufficiency, "INSUFFICIENT")
        self.assertEqual(resp2.data_window.total_active_days, 2)

    def test_stable_performance_trend(self):
        """When changes are within +/- 5 points and pace is steady, report STABLE."""
        # Baseline: 3 days (days 5, 4, 3) around 80.0
        self._create_metric(days_ago=5, accuracy=80.0, memory=80.0, attention=80.0, response_time_ms=3000)
        self._create_metric(days_ago=4, accuracy=81.0, memory=79.0, attention=81.0, response_time_ms=2950)
        self._create_metric(days_ago=3, accuracy=79.0, memory=80.0, attention=80.0, response_time_ms=3050)
        # Recent: 2 days (days 2, 1) around 81.0 (delta ~ +1.0%, well within +/-5.0%)
        self._create_metric(days_ago=2, accuracy=81.0, memory=81.0, attention=81.0, response_time_ms=3000)
        self._create_metric(days_ago=1, accuracy=81.5, memory=80.5, attention=80.5, response_time_ms=2900)

        resp = analyze_patient_performance(self.db, self.patient.id)
        self.assertEqual(resp.data_sufficiency, "SUFFICIENT")
        self.assertEqual(resp.summary.primary_direction, "STABLE")

        obs_by_cat = {o.category: o for o in resp.observations}
        self.assertEqual(obs_by_cat["ACCURACY"].direction, "STABLE")
        self.assertEqual(obs_by_cat["MEMORY"].direction, "STABLE")
        self.assertEqual(obs_by_cat["ATTENTION"].direction, "STABLE")
        self.assertEqual(obs_by_cat["RESPONSE_TIME"].direction, "STABLE")

    def test_improving_accuracy_and_memory(self):
        """Delta >= +5.0 points triggers IMPROVING observation."""
        # Baseline: days 4, 3 at 70.0
        self._create_metric(days_ago=4, accuracy=70.0, memory=68.0, attention=75.0, response_time_ms=3500)
        self._create_metric(days_ago=3, accuracy=70.0, memory=70.0, attention=75.0, response_time_ms=3400)
        # Recent: days 2, 1 at 82.0 (delta = +12.0 points)
        self._create_metric(days_ago=2, accuracy=82.0, memory=80.0, attention=75.0, response_time_ms=3400)
        self._create_metric(days_ago=1, accuracy=82.0, memory=82.0, attention=75.0, response_time_ms=3400)

        resp = analyze_patient_performance(self.db, self.patient.id)
        obs_by_cat = {o.category: o for o in resp.observations}
        self.assertEqual(obs_by_cat["ACCURACY"].direction, "IMPROVING")
        self.assertEqual(obs_by_cat["ACCURACY"].severity, "POSITIVE")
        self.assertGreaterEqual(obs_by_cat["ACCURACY"].evidence.delta, 5.0)

        self.assertEqual(obs_by_cat["MEMORY"].direction, "IMPROVING")
        self.assertEqual(obs_by_cat["MEMORY"].severity, "POSITIVE")

    def test_lower_accuracy_and_attention(self):
        """Delta <= -5.0 points triggers LOWER observation."""
        # Baseline: days 4, 3 at 85.0
        self._create_metric(days_ago=4, accuracy=85.0, memory=85.0, attention=85.0)
        self._create_metric(days_ago=3, accuracy=85.0, memory=85.0, attention=85.0)
        # Recent: days 2, 1 at 74.0 (delta = -11.0 points)
        self._create_metric(days_ago=2, accuracy=74.0, memory=85.0, attention=72.0)
        self._create_metric(days_ago=1, accuracy=74.0, memory=85.0, attention=74.0)

        resp = analyze_patient_performance(self.db, self.patient.id)
        obs_by_cat = {o.category: o for o in resp.observations}
        self.assertEqual(obs_by_cat["ACCURACY"].direction, "LOWER")
        self.assertEqual(obs_by_cat["ACCURACY"].severity, "ATTENTION")
        self.assertEqual(obs_by_cat["ATTENTION"].direction, "LOWER")
        self.assertEqual(obs_by_cat["ATTENTION"].severity, "ATTENTION")

    def test_response_time_faster_and_slower(self):
        """Response time changes >= +500ms or <= -400ms."""
        # Baseline: 3000ms
        self._create_metric(days_ago=4, response_time_ms=3000)
        self._create_metric(days_ago=3, response_time_ms=3000)
        # Recent: 2500ms (delta = -500ms <= -400ms -> FASTER)
        self._create_metric(days_ago=2, response_time_ms=2500)
        self._create_metric(days_ago=1, response_time_ms=2500)

        resp = analyze_patient_performance(self.db, self.patient.id)
        obs_by_cat = {o.category: o for o in resp.observations}
        self.assertEqual(obs_by_cat["RESPONSE_TIME"].direction, "FASTER")
        self.assertEqual(obs_by_cat["RESPONSE_TIME"].severity, "POSITIVE")

    def test_null_metric_category_handling(self):
        """When a category was never played (e.g. attention is NULL), report NOT_ENOUGH_DATA."""
        self._create_metric(days_ago=4, accuracy=80.0, memory=80.0, attention=None)
        self._create_metric(days_ago=3, accuracy=80.0, memory=80.0, attention=None)
        self._create_metric(days_ago=2, accuracy=80.0, memory=80.0, attention=None)
        self._create_metric(days_ago=1, accuracy=80.0, memory=80.0, attention=None)

        resp = analyze_patient_performance(self.db, self.patient.id)
        obs_by_cat = {o.category: o for o in resp.observations}
        self.assertEqual(obs_by_cat["ATTENTION"].direction, "NOT_ENOUGH_DATA")
        self.assertIsNone(obs_by_cat["ATTENTION"].evidence)
        self.assertIn("Not enough", obs_by_cat["ATTENTION"].message)

    def test_deterministic_output(self):
        """Consecutive runs with identical DB data produce identical deterministic results."""
        self._create_metric(days_ago=4, accuracy=75.0, memory=75.0, attention=75.0, response_time_ms=2800)
        self._create_metric(days_ago=3, accuracy=76.0, memory=76.0, attention=76.0, response_time_ms=2850)
        self._create_metric(days_ago=2, accuracy=85.0, memory=86.0, attention=76.0, response_time_ms=2200)
        self._create_metric(days_ago=1, accuracy=84.0, memory=85.0, attention=75.0, response_time_ms=2250)

        fixed_time = datetime(2026, 9, 20, 12, 0, 0, tzinfo=timezone.utc)
        run1 = analyze_patient_performance(self.db, self.patient.id, reference_time=fixed_time)
        run2 = analyze_patient_performance(self.db, self.patient.id, reference_time=fixed_time)

        self.assertEqual(run1.model_dump(), run2.model_dump())

    def test_api_authorization_primary_caregiver(self):
        """Primary caregiver has full access to GET /patients/{id}/analysis."""
        self._create_metric(days_ago=4, accuracy=80.0)
        self._create_metric(days_ago=3, accuracy=80.0)
        self._create_metric(days_ago=1, accuracy=80.0)

        resp = self.client.get(
            f"/patients/{self.patient.id}/analysis",
            headers={"Authorization": f"Bearer {self.primary_token}"},
        )
        self.assertEqual(resp.status_code, 200)
        body = resp.json()
        self.assertEqual(body["patient_id"], str(self.patient.id))
        self.assertEqual(body["data_sufficiency"], "SUFFICIENT")

    def test_api_authorization_doctor_caregiver(self):
        """Assigned secondary/doctor caregiver has full access to GET /patients/{id}/analysis."""
        self._create_metric(days_ago=4, accuracy=80.0)
        self._create_metric(days_ago=3, accuracy=80.0)
        self._create_metric(days_ago=1, accuracy=80.0)

        resp = self.client.get(
            f"/patients/{self.patient.id}/analysis",
            headers={"Authorization": f"Bearer {self.doctor_token}"},
        )
        self.assertEqual(resp.status_code, 200)
        body = resp.json()
        self.assertEqual(body["patient_id"], str(self.patient.id))

    def test_api_authorization_unassigned_caregiver_forbidden(self):
        """Unassigned caregiver gets 404 Patient not found."""
        resp = self.client.get(
            f"/patients/{self.patient.id}/analysis",
            headers={"Authorization": f"Bearer {self.unassigned_token}"},
        )
        self.assertEqual(resp.status_code, 404)

    def test_api_authorization_patient_role_forbidden(self):
        """Patient role token gets 403 Forbidden."""
        resp = self.client.get(
            f"/patients/{self.patient.id}/analysis",
            headers={"Authorization": f"Bearer {self.patient_token}"},
        )
        self.assertEqual(resp.status_code, 403)
