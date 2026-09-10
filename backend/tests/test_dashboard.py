import os
import unittest
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
from app.models.caregiver import Caregiver
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
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


class FakeResult:
    def __init__(self, rows):
        self._rows = rows

    def all(self):
        return self._rows

    def one_or_none(self):
        return self._rows[0] if self._rows else None


class FakeScalars:
    def __init__(self, items):
        self._items = items

    def all(self):
        return self._items


class FakeDashboardSession:
    def __init__(self, store: "DashboardStore"):
        self.store = store

    def close(self):
        pass

    def scalar(self, statement):
        names = _selected_names(statement)
        filters = _equality_filters(statement)
        if names == ("User",):
            return self.store.users.get(filters.get("users.id"))
        if names == ("Caregiver",):
            return self.store.caregivers_by_user_id.get(filters.get("caregivers.user_id"))
        if names == ("Patient",):
            return self.store.patient_if_linked(
                filters.get("patients.id"),
                filters.get("patient_caregivers.caregiver_id"),
            )
        if names == ("display_name",):
            user = self.store.users.get(filters.get("users.id"))
            return None if user is None else user.display_name
        if names == ("PerformanceMetric",):
            return None
        return None

    def execute(self, statement):
        names = _selected_names(statement)
        filters = _equality_filters(statement)
        if names == ("Patient", "display_name"):
            caregiver_id = filters.get("patient_caregivers.caregiver_id")
            rows = []
            for patient_id, caregiver_id_for_link in self.store.links:
                if caregiver_id_for_link != caregiver_id:
                    continue
                patient = self.store.patients[patient_id]
                rows.append((patient, self.store.users[patient.user_id].display_name))
            return FakeResult(rows)
        if names == ("PatientCaregiver", "Caregiver", "display_name"):
            patient_id = filters.get("patient_caregivers.patient_id")
            caregiver_id = filters.get("patient_caregivers.caregiver_id")
            link = self.store.link_objects.get((patient_id, caregiver_id))
            caregiver = self.store.caregivers.get(caregiver_id)
            if link is None or caregiver is None:
                return FakeResult([])
            caregiver_user = self.store.users[caregiver.user_id]
            return FakeResult([(link, caregiver, caregiver_user.display_name)])
        return FakeResult([])

    def scalars(self, statement):
        return FakeScalars([])


class DashboardStore:
    def __init__(self):
        self.users: dict = {}
        self.caregivers: dict = {}
        self.caregivers_by_user_id: dict = {}
        self.patients: dict = {}
        self.links: set[tuple] = set()
        self.link_objects: dict = {}

    def add_user(self, user: User) -> User:
        self.users[user.id] = user
        return user

    def add_caregiver(self, caregiver: Caregiver) -> Caregiver:
        self.caregivers[caregiver.id] = caregiver
        self.caregivers_by_user_id[caregiver.user_id] = caregiver
        return caregiver

    def add_patient(self, patient: Patient) -> Patient:
        self.patients[patient.id] = patient
        return patient

    def link(self, patient: Patient, caregiver: Caregiver, is_primary: bool = True) -> PatientCaregiver:
        pair = (patient.id, caregiver.id)
        self.links.add(pair)
        record = PatientCaregiver(
            id=uuid4(),
            patient_id=patient.id,
            caregiver_id=caregiver.id,
            is_primary=is_primary,
        )
        self.link_objects[pair] = record
        return record

    def patient_if_linked(self, patient_id, caregiver_id):
        if (patient_id, caregiver_id) in self.links:
            return self.patients.get(patient_id)
        return None


def _make_user(role: str, email: str, display_name: str) -> User:
    return User(
        id=uuid4(),
        email=email,
        password_hash=hash_password("secret"),
        display_name=display_name,
        role=role,
        is_active=True,
    )


class CaregiverDashboardAuthorizationTests(unittest.TestCase):
    def setUp(self):
        self.store = DashboardStore()
        self.caregiver_user = self.store.add_user(
            _make_user("CAREGIVER", "caregiver@example.com", "Demo Caregiver")
        )
        self.patient_user = self.store.add_user(
            _make_user("PATIENT", "patient@example.com", "Demo Patient")
        )
        self.other_caregiver_user = self.store.add_user(
            _make_user("CAREGIVER", "other.caregiver@example.com", "Other Caregiver")
        )
        self.other_patient_user = self.store.add_user(
            _make_user("PATIENT", "other.patient@example.com", "Unrelated Patient")
        )
        self.caregiver = self.store.add_caregiver(
            Caregiver(id=uuid4(), user_id=self.caregiver_user.id, caregiver_type="FAMILY")
        )
        self.other_caregiver = self.store.add_caregiver(
            Caregiver(id=uuid4(), user_id=self.other_caregiver_user.id, caregiver_type="DOCTOR")
        )
        self.patient = self.store.add_patient(
            Patient(
                id=uuid4(),
                user_id=self.patient_user.id,
                preferred_language="English",
                timezone="Asia/Kolkata",
                profile_photo_ref=None,
                date_of_birth=None,
                gender="FEMALE",
            )
        )
        self.other_patient = self.store.add_patient(
            Patient(
                id=uuid4(),
                user_id=self.other_patient_user.id,
                preferred_language="English",
                timezone="Asia/Kolkata",
                profile_photo_ref=None,
            )
        )
        self.store.link(self.patient, self.caregiver)
        self.store.link(self.other_patient, self.other_caregiver)
        app.dependency_overrides[get_db] = self._override_get_db
        self.client = TestClient(app)

    def tearDown(self):
        app.dependency_overrides.clear()
        self.client.close()

    def _override_get_db(self):
        yield FakeDashboardSession(self.store)

    def _auth_header(self, user: User) -> dict[str, str]:
        return {"Authorization": f"Bearer {create_access_token(user.id)}"}

    def test_unauthenticated_patients_returns_401(self):
        response = self.client.get("/patients")
        self.assertEqual(response.status_code, 401)

    def test_unauthenticated_dashboard_returns_401(self):
        response = self.client.get(f"/patients/{self.patient.id}/dashboard")
        self.assertEqual(response.status_code, 401)

    def test_unauthenticated_performance_returns_401(self):
        response = self.client.get(f"/patients/{self.patient.id}/performance")
        self.assertEqual(response.status_code, 401)

    def test_authenticated_caregiver_lists_only_linked_patients(self):
        response = self.client.get("/patients", headers=self._auth_header(self.caregiver_user))
        self.assertEqual(response.status_code, 200)
        patients = response.json()["patients"]
        self.assertEqual([item["id"] for item in patients], [str(self.patient.id)])
        self.assertEqual(patients[0]["display_name"], "Demo Patient")

    def test_authenticated_caregiver_can_retrieve_patient_dashboard(self):
        response = self.client.get(
            f"/patients/{self.patient.id}/dashboard",
            headers=self._auth_header(self.caregiver_user),
        )
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["patient"]["id"], str(self.patient.id))
        self.assertEqual(body["caregiver_relationship"]["caregiver_id"], str(self.caregiver.id))

    def test_authenticated_caregiver_can_retrieve_patient_performance(self):
        response = self.client.get(
            f"/patients/{self.patient.id}/performance",
            headers=self._auth_header(self.caregiver_user),
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["patient_id"], str(self.patient.id))
        self.assertEqual(response.json()["metrics"], [])

    def test_authenticated_patient_cannot_access_caregiver_dashboard(self):
        headers = self._auth_header(self.patient_user)
        self.assertEqual(self.client.get("/patients", headers=headers).status_code, 403)
        self.assertEqual(
            self.client.get(f"/patients/{self.patient.id}/dashboard", headers=headers).status_code,
            403,
        )
        self.assertEqual(
            self.client.get(f"/patients/{self.patient.id}/performance", headers=headers).status_code,
            403,
        )

    def test_authenticated_caregiver_cannot_access_unrelated_patient(self):
        headers = self._auth_header(self.caregiver_user)
        self.assertEqual(
            self.client.get(
                f"/patients/{self.other_patient.id}/dashboard",
                headers=headers,
            ).status_code,
            404,
        )
        self.assertEqual(
            self.client.get(
                f"/patients/{self.other_patient.id}/performance",
                headers=headers,
            ).status_code,
            404,
        )

    def test_query_parameter_cannot_impersonate_another_caregiver(self):
        headers = self._auth_header(self.caregiver_user)
        impersonation = f"demo_caregiver_id={self.other_caregiver.id}"
        listed = self.client.get(f"/patients?{impersonation}", headers=headers)
        self.assertEqual(listed.status_code, 200)
        self.assertEqual(
            [item["id"] for item in listed.json()["patients"]],
            [str(self.patient.id)],
        )
        self.assertEqual(
            self.client.get(
                f"/patients/{self.other_patient.id}/dashboard?{impersonation}",
                headers=headers,
            ).status_code,
            404,
        )
        self.assertEqual(
            self.client.get(
                f"/patients/{self.other_patient.id}/performance?{impersonation}",
                headers=headers,
            ).status_code,
            404,
        )

    def test_dashboard_endpoints_do_not_require_demo_caregiver_id(self):
        headers = self._auth_header(self.caregiver_user)
        self.assertEqual(self.client.get("/patients", headers=headers).status_code, 200)
        self.assertEqual(
            self.client.get(
                f"/patients/{self.patient.id}/dashboard",
                headers=headers,
            ).status_code,
            200,
        )
        self.assertEqual(
            self.client.get(
                f"/patients/{self.patient.id}/performance",
                headers=headers,
            ).status_code,
            200,
        )
