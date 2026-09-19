import os
import unittest
from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal
from uuid import UUID, uuid4
from zoneinfo import ZoneInfo

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
from app.models.game import Game
from app.models.game_result import GameResult
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.models.performance_metric import PerformanceMetric
from app.models.user import User
from app.scripts.seed_demo import seed_demo_data
from app.security import create_access_token, hash_password
from app.services.performance import (
    calculate_and_upsert_daily_performance,
    get_local_day_utc_boundaries,
    get_patient_tz,
    record_daily_performance_metric,
)


class PerformanceMetricDailyAggregationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        seed_demo_data()

    def setUp(self):
        self.client = TestClient(app)
        self.db = SessionLocal()

        # Find existing seeded games
        self.memory_game = self.db.scalar(
            select(Game).where(Game.category == "MEMORY", Game.is_active.is_(True))
        )
        self.attention_game = self.db.scalar(
            select(Game).where(Game.category == "CONCENTRATION_ATTENTION", Game.is_active.is_(True))
        )
        self.assertIsNotNone(self.memory_game)
        self.assertIsNotNone(self.attention_game)

        # Create isolated test patient user and caregiver user
        self.unique_suffix = uuid4().hex[:8]
        self.patient_user = User(
            id=uuid4(),
            email=f"perf.patient.{self.unique_suffix}@example.com",
            password_hash=hash_password("Password123!"),
            display_name=f"Perf Patient {self.unique_suffix}",
            role="PATIENT",
            is_active=True,
        )
        self.caregiver_user = User(
            id=uuid4(),
            email=f"perf.caregiver.{self.unique_suffix}@example.com",
            password_hash=hash_password("Password123!"),
            display_name=f"Perf Caregiver {self.unique_suffix}",
            role="CAREGIVER",
            is_active=True,
        )
        self.db.add_all([self.patient_user, self.caregiver_user])
        self.db.flush()

        self.patient = Patient(
            id=uuid4(),
            user_id=self.patient_user.id,
            timezone="Asia/Kolkata",
            preferred_language="English",
        )
        self.caregiver = Caregiver(
            id=uuid4(),
            user_id=self.caregiver_user.id,
            caregiver_type="FAMILY",
        )
        self.db.add_all([self.patient, self.caregiver])
        self.db.flush()

        self.link = PatientCaregiver(
            patient_id=self.patient.id,
            caregiver_id=self.caregiver.id,
            is_primary=True,
        )
        self.db.add(self.link)
        self.db.commit()

        self.patient_token = create_access_token(self.patient_user.id)
        self.caregiver_token = create_access_token(self.caregiver_user.id)

    def tearDown(self):
        try:
            self.db.rollback()
        except Exception:
            pass
        patient_id = self.patient.id
        caregiver_id = self.caregiver.id
        user_ids = [self.patient_user.id, self.caregiver_user.id]
        # Clean up database records created for this patient
        self.db.execute(delete(GameResult).where(GameResult.session_id.in_(
            select(GameSession.id).where(GameSession.patient_id == patient_id)
        )))
        self.db.execute(delete(GameSession).where(GameSession.patient_id == patient_id))
        self.db.execute(delete(PerformanceMetric).where(PerformanceMetric.patient_id == patient_id))
        self.db.execute(delete(PatientCaregiver).where(PatientCaregiver.patient_id == patient_id))
        self.db.execute(delete(Patient).where(Patient.id == patient_id))
        self.db.execute(delete(Caregiver).where(Caregiver.id == caregiver_id))
        self.db.execute(delete(User).where(User.id.in_(user_ids)))
        self.db.commit()
        self.db.close()
        self.client.close()

    def _auth(self, token: str) -> dict[str, str]:
        return {"Authorization": f"Bearer {token}"}

    def _start_and_submit_game(
        self,
        game_id: UUID,
        accuracy: float | None = 85.0,
        response_time_ms: int | None = 1000,
        score: float = 85.0,
        difficulty_level: int = 1,
    ) -> dict:
        start_res = self.client.post(
            f"/games/{game_id}/sessions",
            headers=self._auth(self.patient_token),
            json={"difficulty_level": difficulty_level},
        )
        self.assertEqual(start_res.status_code, 201)
        session_id = start_res.json()["id"]

        result_payload = {
            "score": score,
            "accuracy": accuracy,
            "response_time_ms": response_time_ms,
            "correct_answers": 8 if accuracy else None,
            "total_questions": 10 if accuracy else None,
            "mistakes": 2 if accuracy else None,
        }
        res = self.client.post(
            f"/games/sessions/{session_id}/result",
            headers=self._auth(self.patient_token),
            json=result_payload,
        )
        return {"session_id": session_id, "result_response": res}

    def test_01_first_completed_memory_game_creates_one_metric_row(self):
        """Rule 1, 2, 6, 7: First completed MEMORY game creates one metric row."""
        res = self._start_and_submit_game(
            self.memory_game.id, accuracy=80.0, response_time_ms=1050, score=80.0
        )["result_response"]
        self.assertEqual(res.status_code, 201)

        metrics = self.db.scalars(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        ).all()
        self.assertEqual(len(metrics), 1)
        metric = metrics[0]

        # Verify today's date in patient's timezone
        now_utc = datetime.now(timezone.utc)
        expected_date = now_utc.astimezone(ZoneInfo("Asia/Kolkata")).date()
        self.assertEqual(metric.metric_date, expected_date)

        self.assertEqual(metric.memory_score, Decimal("80.00"))
        self.assertIsNone(metric.attention_score)  # Missing category remains NULL
        self.assertEqual(metric.average_accuracy, Decimal("80.00"))
        self.assertEqual(metric.average_response_time_ms, 1050)
        self.assertEqual(metric.games_completed, 1)
        self.assertEqual(metric.total_sessions, 1)

    def test_02_second_completed_memory_game_updates_same_daily_row(self):
        """Rule 1: Second completed game on same day updates the existing metric row."""
        self._start_and_submit_game(
            self.memory_game.id, accuracy=80.0, response_time_ms=1000, score=80.0
        )
        self._start_and_submit_game(
            self.memory_game.id, accuracy=90.0, response_time_ms=800, score=90.0
        )

        metrics = self.db.scalars(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        ).all()
        self.assertEqual(len(metrics), 1)
        metric = metrics[0]

        self.assertEqual(metric.games_completed, 2)
        self.assertEqual(metric.total_sessions, 2)
        # Average: (80 + 90) / 2 = 85.00
        self.assertEqual(metric.memory_score, Decimal("85.00"))
        self.assertEqual(metric.average_accuracy, Decimal("85.00"))
        # Average response time: (1000 + 800) / 2 = 900
        self.assertEqual(metric.average_response_time_ms, 900)
        self.assertIsNone(metric.attention_score)

    def test_03_memory_accuracy_averages_correctly(self):
        """Rule 2: memory_score is average accuracy of MEMORY games with 2 decimals."""
        self._start_and_submit_game(self.memory_game.id, accuracy=70.0)
        self._start_and_submit_game(self.memory_game.id, accuracy=80.0)
        self._start_and_submit_game(self.memory_game.id, accuracy=95.0)

        metric = self.db.scalar(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        )
        self.assertIsNotNone(metric)
        # (70 + 80 + 95) / 3 = 245 / 3 = 81.6666... -> 81.67
        self.assertEqual(metric.memory_score, Decimal("81.67"))
        self.assertEqual(metric.average_accuracy, Decimal("81.67"))

    def test_04_attention_accuracy_averages_correctly(self):
        """Rule 3: attention_score is average accuracy of CONCENTRATION_ATTENTION games."""
        self._start_and_submit_game(self.attention_game.id, accuracy=65.0)
        self._start_and_submit_game(self.attention_game.id, accuracy=85.0)

        metric = self.db.scalar(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        )
        self.assertIsNotNone(metric)
        # (65 + 85) / 2 = 75.00
        self.assertEqual(metric.attention_score, Decimal("75.00"))
        self.assertIsNone(metric.memory_score)
        self.assertEqual(metric.average_accuracy, Decimal("75.00"))

    def test_05_memory_and_attention_categories_remain_separate(self):
        """Rule 2, 3, 4: Memory and Attention remain distinct, average_accuracy combines both."""
        self._start_and_submit_game(self.memory_game.id, accuracy=90.0)
        self._start_and_submit_game(self.attention_game.id, accuracy=70.0)

        metric = self.db.scalar(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        )
        self.assertIsNotNone(metric)
        self.assertEqual(metric.memory_score, Decimal("90.00"))
        self.assertEqual(metric.attention_score, Decimal("70.00"))
        # Overall average: (90 + 70) / 2 = 80.00
        self.assertEqual(metric.average_accuracy, Decimal("80.00"))

    def test_06_missing_category_remains_null(self):
        """Rule 8: Missing category must remain NULL (never 0)."""
        self._start_and_submit_game(self.attention_game.id, accuracy=78.5)

        metric = self.db.scalar(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        )
        self.assertIsNotNone(metric)
        self.assertEqual(metric.attention_score, Decimal("78.50"))
        self.assertIsNone(metric.memory_score)
        self.assertNotEqual(metric.memory_score, 0)
        self.assertNotEqual(metric.memory_score, Decimal("0.00"))

    def test_07_null_accuracy_is_ignored_in_averages(self):
        """Rule 9: NULL accuracy values are ignored when calculating averages (never 0)."""
        self._start_and_submit_game(self.memory_game.id, accuracy=82.0)
        self._start_and_submit_game(self.memory_game.id, accuracy=None)

        metric = self.db.scalar(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        )
        self.assertIsNotNone(metric)
        self.assertEqual(metric.games_completed, 2)
        # Average ignores None, so average of [82.0] = 82.00, NOT (82 + 0) / 2 = 41
        self.assertEqual(metric.memory_score, Decimal("82.00"))
        self.assertEqual(metric.average_accuracy, Decimal("82.00"))

    def test_08_null_response_time_is_ignored_in_response_time_average(self):
        """Rule 9: NULL response_time_ms is ignored in average_response_time_ms."""
        self._start_and_submit_game(self.memory_game.id, response_time_ms=1400)
        self._start_and_submit_game(self.memory_game.id, response_time_ms=None)

        metric = self.db.scalar(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        )
        self.assertIsNotNone(metric)
        self.assertEqual(metric.average_response_time_ms, 1400)

    def test_09_games_completed_increments_correctly(self):
        """Rule 6: games_completed accurately counts completed sessions."""
        self._start_and_submit_game(self.memory_game.id)
        self._start_and_submit_game(self.attention_game.id)

        metric = self.db.scalar(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        )
        self.assertEqual(metric.games_completed, 2)

    def test_10_total_sessions_increments_correctly(self):
        """Rule 7: total_sessions counts started sessions while games_completed counts completed."""
        # Start session 1 and complete it
        self._start_and_submit_game(self.memory_game.id)
        # Start session 2 and leave it uncompleted
        start_res = self.client.post(
            f"/games/{self.memory_game.id}/sessions",
            headers=self._auth(self.patient_token),
            json={"difficulty_level": 1},
        )
        self.assertEqual(start_res.status_code, 201)

        # Re-trigger calculation via service function
        record_daily_performance_metric(
            self.db,
            session=self.db.scalar(select(GameSession).where(GameSession.id == UUID(start_res.json()["id"]))),
            patient=self.patient,
        )
        self.db.commit()

        metric = self.db.scalar(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        )
        self.assertEqual(metric.games_completed, 1)
        self.assertEqual(metric.total_sessions, 2)

    def test_11_patient_timezone_determines_metric_date_correctly(self):
        """Rule 10: metric_date respects patient's stored timezone."""
        # Update patient timezone to Tokyo (UTC+9)
        self.patient.timezone = "Asia/Tokyo"
        self.db.commit()

        # Create session at 22:00 UTC on 2026-06-15.
        # In Tokyo (UTC+9), 22:00 UTC is 07:00 on 2026-06-16.
        tokyo_test_time = datetime(2026, 6, 15, 22, 0, 0, tzinfo=timezone.utc)
        session_id = uuid4()
        session = GameSession(
            id=session_id,
            patient_id=self.patient.id,
            game_id=self.memory_game.id,
            difficulty_level=1,
            started_at=tokyo_test_time,
            completed_at=tokyo_test_time,
            status="COMPLETED",
        )
        result = GameResult(
            session_id=session_id,
            score=Decimal("88.00"),
            accuracy=Decimal("88.00"),
            response_time_ms=900,
        )
        self.db.add_all([session, result])
        self.db.flush()

        calculate_and_upsert_daily_performance(
            db=self.db,
            patient_id=self.patient.id,
            patient_tz_name=self.patient.timezone,
            reference_time=tokyo_test_time,
        )
        self.db.commit()

        metric = self.db.scalar(
            select(PerformanceMetric).where(
                PerformanceMetric.patient_id == self.patient.id,
                PerformanceMetric.metric_date == date(2026, 6, 16),
            )
        )
        self.assertIsNotNone(metric)
        self.assertEqual(metric.metric_date, date(2026, 6, 16))

        # Test invalid timezone fallback to Asia/Kolkata
        tz = get_patient_tz("Invalid/Fake_Timezone")
        self.assertEqual(tz.key, "Asia/Kolkata")

    def test_12_midnight_crossing_session_preserves_db_constraint(self):
        """Rule 7: Midnight-crossing session doesn't violate ck_performance_metrics_games_lte_sessions."""
        self.patient.timezone = "UTC"
        self.db.commit()

        # Session starts 23:55 on Day 1 and finishes 00:05 on Day 2.
        # On Day 2, started_count is 0, but games_completed is 1.
        session_id = uuid4()
        session = GameSession(
            id=session_id,
            patient_id=self.patient.id,
            game_id=self.memory_game.id,
            difficulty_level=1,
            started_at=datetime(2026, 8, 10, 23, 55, 0, tzinfo=timezone.utc),
            completed_at=datetime(2026, 8, 11, 0, 5, 0, tzinfo=timezone.utc),
            status="COMPLETED",
        )
        result = GameResult(
            session_id=session_id,
            score=Decimal("95.00"),
            accuracy=Decimal("95.00"),
            response_time_ms=850,
        )
        self.db.add_all([session, result])
        self.db.flush()

        metric = calculate_and_upsert_daily_performance(
            db=self.db,
            patient_id=self.patient.id,
            patient_tz_name=self.patient.timezone,
            reference_time=datetime(2026, 8, 11, 0, 5, 0, tzinfo=timezone.utc),
        )
        self.db.commit()

        self.assertEqual(metric.metric_date, date(2026, 8, 11))
        self.assertEqual(metric.games_completed, 1)
        # Invariant preserved: games_completed <= total_sessions
        self.assertEqual(metric.total_sessions, 1)
        self.assertGreaterEqual(metric.total_sessions, metric.games_completed)

    def test_13_duplicate_result_submission_rejected_and_idempotent(self):
        """Rule 12: Duplicate result submission rejected with 409 and idempotently recalculated."""
        start_res = self.client.post(
            f"/games/{self.memory_game.id}/sessions",
            headers=self._auth(self.patient_token),
            json={"difficulty_level": 1},
        )
        session_id = start_res.json()["id"]

        result_payload = {"score": 80.0, "accuracy": 80.0, "response_time_ms": 1000}
        first_submit = self.client.post(
            f"/games/sessions/{session_id}/result",
            headers=self._auth(self.patient_token),
            json=result_payload,
        )
        self.assertEqual(first_submit.status_code, 201)

        # Duplicate submission is rejected with 409
        second_submit = self.client.post(
            f"/games/sessions/{session_id}/result",
            headers=self._auth(self.patient_token),
            json=result_payload,
        )
        self.assertEqual(second_submit.status_code, 409)

        # Recomputing directly produces the identical row and values
        session = self.db.scalar(select(GameSession).where(GameSession.id == UUID(session_id)))
        metric = record_daily_performance_metric(self.db, session=session, patient=self.patient)
        self.db.commit()

        self.assertEqual(metric.games_completed, 1)
        self.assertEqual(metric.memory_score, Decimal("80.00"))

    def test_14_existing_dashboard_and_performance_endpoints_return_valid_data(self):
        """Caregiver dashboard and performance endpoints read newly created performance metrics."""
        self._start_and_submit_game(
            self.memory_game.id, accuracy=84.0, response_time_ms=950, score=84.0
        )

        # 1. GET /patients/{patient_id}/performance
        perf_res = self.client.get(
            f"/patients/{self.patient.id}/performance",
            headers=self._auth(self.caregiver_token),
        )
        self.assertEqual(perf_res.status_code, 200)
        data = perf_res.json()
        self.assertIn("metrics", data)
        self.assertGreaterEqual(len(data["metrics"]), 1)
        point = data["metrics"][-1]
        self.assertEqual(float(point["memory_score"]), 84.0)
        self.assertIsNone(point["attention_score"])
        self.assertEqual(float(point["average_accuracy"]), 84.0)
        self.assertEqual(point["average_response_time_ms"], 950)
        self.assertEqual(point["games_completed"], 1)

        # 2. GET /patients/{patient_id}/dashboard
        dash_res = self.client.get(
            f"/patients/{self.patient.id}/dashboard",
            headers=self._auth(self.caregiver_token),
        )
        self.assertEqual(dash_res.status_code, 200)
        dash_data = dash_res.json()
        self.assertIsNotNone(dash_data["latest_performance"])
        self.assertEqual(float(dash_data["latest_performance"]["memory_score"]), 84.0)

    def test_15_pattern_recall_contributes_to_memory_performance_aggregation(self):
        """Verify that PATTERN_RECALL contributes to MEMORY score and updates games_completed."""
        pattern_game = self.db.scalar(
            select(Game).where(Game.code == "PATTERN_RECALL", Game.is_active.is_(True))
        )
        self.assertIsNotNone(pattern_game)
        self.assertEqual(pattern_game.category, "MEMORY")

        # Submit a pattern recall game result with Android telemetry fields
        res = self._start_and_submit_game(
            pattern_game.id,
            accuracy=92.0,
            response_time_ms=1200,
            score=92.0,
            difficulty_level=2,
        )["result_response"]
        self.assertEqual(res.status_code, 201)

        metric = self.db.scalar(
            select(PerformanceMetric).where(PerformanceMetric.patient_id == self.patient.id)
        )
        self.assertIsNotNone(metric)
        self.assertEqual(metric.games_completed, 1)
        self.assertEqual(metric.memory_score, Decimal("92.00"))
        self.assertIsNone(metric.attention_score)
        self.assertEqual(metric.average_accuracy, Decimal("92.00"))
        self.assertEqual(metric.average_response_time_ms, 1200)


if __name__ == "__main__":
    unittest.main()
