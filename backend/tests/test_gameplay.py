import os
import unittest
from datetime import datetime, timezone
from uuid import uuid4

from sqlalchemy.sql.elements import BindParameter, BinaryExpression
from sqlalchemy.sql.operators import eq
from sqlalchemy.sql.visitors import iterate

os.environ.setdefault("JWT_SECRET_KEY", "test-only-secret")
os.environ.setdefault("POSTGRES_USER", "sih2026")
os.environ.setdefault("POSTGRES_PASSWORD", "test")
os.environ.setdefault("POSTGRES_HOST", "localhost")
os.environ.setdefault("POSTGRES_PORT", "5432")
os.environ.setdefault("POSTGRES_DB", "sih2026")

from fastapi.testclient import TestClient

from app.api.dependencies import get_db
from app.main import app
from app.models.game import Game
from app.models.game_result import GameResult
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.user import User
from app.security import create_access_token, hash_password


def _equality_filters(statement) -> dict[str, object]:
    filters: dict[str, object] = {}
    where = getattr(statement, "whereclause", None)
    if where is None:
        return filters
    for clause in iterate(where, {}):
        if not isinstance(clause, BinaryExpression) or clause.operator is not eq:
            continue
        left, right = clause.left, clause.right
        column = left if getattr(getattr(left, "table", None), "name", None) else right
        value_expr = right if column is left else left
        table_name = getattr(getattr(column, "table", None), "name", None)
        column_key = getattr(column, "key", None)
        if table_name is None or column_key is None:
            continue
        value = value_expr.value if isinstance(value_expr, BindParameter) else getattr(
            value_expr, "value", value_expr
        )
        filters[f"{table_name}.{column_key}"] = value
    return filters


def _selected_names(statement) -> tuple[str, ...]:
    return tuple(column["name"] for column in statement.column_descriptions)


class FakeScalars:
    def __init__(self, items):
        self._items = items

    def all(self):
        return self._items


class FakeGameplaySession:
    def __init__(self, store: "GameplayStore"):
        self.store = store

    def close(self):
        pass

    def commit(self):
        pass

    def flush(self):
        pass

    def refresh(self, obj):
        pass

    def add(self, obj):
        if isinstance(obj, GameSession):
            if obj.id is None:
                obj.id = uuid4()
            self.store.sessions[obj.id] = obj
        elif isinstance(obj, GameResult):
            if obj.id is None:
                obj.id = uuid4()
            self.store.results[obj.session_id] = obj

    def scalar(self, statement):
        names = _selected_names(statement)
        filters = _equality_filters(statement)
        if names == ("User",):
            return self.store.users.get(filters.get("users.id"))
        if names == ("Patient",):
            return self.store.patients_by_user_id.get(filters.get("patients.user_id"))
        if names == ("Game",):
            game = self.store.games.get(filters.get("games.id"))
            if game is None or not game.is_active:
                return None
            return game
        if names == ("GameSession",):
            return self.store.sessions.get(filters.get("game_sessions.id"))
        if names == ("GameResult",):
            return self.store.results.get(filters.get("game_results.session_id"))
        return None

    def scalars(self, statement):
        names = _selected_names(statement)
        if names == ("Game",):
            games = [game for game in self.store.games.values() if game.is_active]
            games.sort(key=lambda game: (game.name, str(game.id)))
            return FakeScalars(games)
        return FakeScalars([])


class GameplayStore:
    def __init__(self):
        self.users: dict = {}
        self.patients_by_user_id: dict = {}
        self.patients: dict = {}
        self.games: dict = {}
        self.sessions: dict = {}
        self.results: dict = {}

    def add_user(self, user: User) -> User:
        self.users[user.id] = user
        return user

    def add_patient(self, patient: Patient) -> Patient:
        self.patients[patient.id] = patient
        self.patients_by_user_id[patient.user_id] = patient
        return patient

    def add_game(self, game: Game) -> Game:
        self.games[game.id] = game
        return game

    def add_session(self, session: GameSession) -> GameSession:
        self.sessions[session.id] = session
        return session


def _make_user(role: str, email: str, display_name: str) -> User:
    return User(
        id=uuid4(),
        email=email,
        password_hash=hash_password("secret"),
        display_name=display_name,
        role=role,
        is_active=True,
    )


class PatientGameplayTests(unittest.TestCase):
    def setUp(self):
        self.store = GameplayStore()
        self.patient_user = self.store.add_user(
            _make_user("PATIENT", "patient@example.com", "Demo Patient")
        )
        self.other_patient_user = self.store.add_user(
            _make_user("PATIENT", "other.patient@example.com", "Other Patient")
        )
        self.caregiver_user = self.store.add_user(
            _make_user("CAREGIVER", "caregiver@example.com", "Demo Caregiver")
        )
        self.patient = self.store.add_patient(
            Patient(id=uuid4(), user_id=self.patient_user.id, preferred_language="English")
        )
        self.other_patient = self.store.add_patient(
            Patient(id=uuid4(), user_id=self.other_patient_user.id, preferred_language="English")
        )
        self.game = self.store.add_game(
            Game(
                id=uuid4(),
                code="DAILY_RECALL",
                name="Daily Recall",
                category="MEMORY",
                description="A short memory activity.",
                is_active=True,
            )
        )
        self.inactive_game = self.store.add_game(
            Game(
                id=uuid4(),
                code="INACTIVE_GAME",
                name="Inactive Game",
                category="MEMORY",
                is_active=False,
            )
        )
        self.other_session = self.store.add_session(
            GameSession(
                id=uuid4(),
                patient_id=self.other_patient.id,
                game_id=self.game.id,
                difficulty_level=1,
                started_at=datetime.now(timezone.utc),
                status="STARTED",
            )
        )
        app.dependency_overrides[get_db] = self._override_get_db
        self.client = TestClient(app)

    def tearDown(self):
        app.dependency_overrides.clear()
        self.client.close()

    def _override_get_db(self):
        yield FakeGameplaySession(self.store)

    def _auth_header(self, user: User) -> dict[str, str]:
        return {"Authorization": f"Bearer {create_access_token(user.id)}"}

    def test_authenticated_patient_can_list_games(self):
        response = self.client.get("/games", headers=self._auth_header(self.patient_user))
        self.assertEqual(response.status_code, 200)
        games = response.json()["games"]
        self.assertEqual(len(games), 1)
        self.assertEqual(games[0]["id"], str(self.game.id))
        self.assertEqual(games[0]["code"], "DAILY_RECALL")
        self.assertNotIn(str(self.inactive_game.id), [item["id"] for item in games])

    def test_unauthenticated_games_request_is_rejected(self):
        response = self.client.get("/games")
        self.assertEqual(response.status_code, 401)

    def test_authenticated_patient_can_start_valid_game_session(self):
        response = self.client.post(
            f"/games/{self.game.id}/sessions",
            headers=self._auth_header(self.patient_user),
            json={"difficulty_level": 2},
        )
        self.assertEqual(response.status_code, 201)
        body = response.json()
        self.assertEqual(body["game_id"], str(self.game.id))
        self.assertEqual(body["patient_id"], str(self.patient.id))
        self.assertEqual(body["difficulty_level"], 2)
        self.assertEqual(body["status"], "STARTED")
        self.assertIsNone(body["completed_at"])

    def test_starting_nonexistent_game_is_rejected(self):
        response = self.client.post(
            f"/games/{uuid4()}/sessions",
            headers=self._auth_header(self.patient_user),
            json={},
        )
        self.assertEqual(response.status_code, 404)

    def test_authenticated_patient_can_submit_result_for_own_session(self):
        start = self.client.post(
            f"/games/{self.game.id}/sessions",
            headers=self._auth_header(self.patient_user),
            json={},
        )
        session_id = start.json()["id"]
        response = self.client.post(
            f"/games/sessions/{session_id}/result",
            headers=self._auth_header(self.patient_user),
            json={
                "score": 88.5,
                "accuracy": 92.0,
                "correct_answers": 11,
                "total_questions": 12,
                "response_time_ms": 980,
                "mistakes": 1,
            },
        )
        self.assertEqual(response.status_code, 201)
        body = response.json()
        self.assertEqual(body["session_id"], session_id)
        self.assertEqual(body["session_status"], "COMPLETED")
        self.assertIsNotNone(body["completed_at"])
        self.assertEqual(float(body["score"]), 88.5)

    def test_patient_cannot_submit_result_for_another_patient_session(self):
        response = self.client.post(
            f"/games/sessions/{self.other_session.id}/result",
            headers=self._auth_header(self.patient_user),
            json={"score": 50},
        )
        self.assertEqual(response.status_code, 404)

    def test_nonexistent_session_is_rejected(self):
        response = self.client.post(
            f"/games/sessions/{uuid4()}/result",
            headers=self._auth_header(self.patient_user),
            json={"score": 50},
        )
        self.assertEqual(response.status_code, 404)

    def test_caregiver_cannot_start_patient_session(self):
        response = self.client.post(
            f"/games/{self.game.id}/sessions",
            headers=self._auth_header(self.caregiver_user),
            json={},
        )
        self.assertEqual(response.status_code, 403)
