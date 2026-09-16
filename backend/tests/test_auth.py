import os
import unittest
from datetime import timedelta
from uuid import uuid4

import jwt
os.environ.setdefault("JWT_SECRET_KEY", "test-only-secret-32-bytes-long-123456")
os.environ.setdefault("POSTGRES_USER", "sih2026")
os.environ.setdefault("POSTGRES_PASSWORD", "sih2026_dev_snag")
os.environ.setdefault("POSTGRES_HOST", "localhost")
os.environ.setdefault("POSTGRES_PORT", "5432")
os.environ.setdefault("POSTGRES_DB", "sih2026")

from fastapi import HTTPException
from fastapi.security import HTTPAuthorizationCredentials
from app.api.auth import current_user, login
from app.api.dependencies import get_current_user
from app.models.user import User
from app.security import (
    create_access_token,
    hash_password,
    verify_password,
)


class FakeSession:
    def __init__(self, user: User | None):
        self.user = user

    def scalar(self, statement):
        return self.user

    def close(self):
        pass


class AuthenticationTests(unittest.TestCase):
    def test_password_hashing_and_verification(self):
        hashed = hash_password("correct horse battery staple")
        self.assertNotEqual(hashed, "correct horse battery staple")
        self.assertTrue(verify_password("correct horse battery staple", hashed))
        self.assertFalse(verify_password("wrong password", hashed))

    def test_login_and_current_user(self):
        user = User(
            id=uuid4(),
            email="demo@example.com",
            password_hash=hash_password("secret"),
            display_name="Demo User",
            role="CAREGIVER",
            is_active=True,
        )
        response = login(
            type("Credentials", (), {"email": "demo@example.com", "password": "secret"})(),
            FakeSession(user),
        )
        self.assertEqual(response.token_type, "bearer")
        token = response.access_token

        me = current_user(
            get_current_user(
                HTTPAuthorizationCredentials(scheme="Bearer", credentials=token),
                FakeSession(user),
            )
        )
        self.assertEqual(me.email, "demo@example.com")

    def test_invalid_password_and_expired_token(self):
        user = User(
            id=uuid4(),
            email="demo@example.com",
            password_hash=hash_password("secret"),
            display_name="Demo User",
            role="CAREGIVER",
            is_active=True,
        )
        with self.assertRaises(HTTPException) as login_error:
            login(
                type("Credentials", (), {"email": "demo@example.com", "password": "wrong"})(),
                FakeSession(user),
            )
        self.assertEqual(login_error.exception.status_code, 401)
        self.assertEqual(login_error.exception.detail, "Invalid email or password")

        expired_token = create_access_token(user.id, timedelta(seconds=-1))
        with self.assertRaises(HTTPException) as token_error:
            get_current_user(
                HTTPAuthorizationCredentials(scheme="Bearer", credentials=expired_token),
                FakeSession(user),
            )
        self.assertEqual(token_error.exception.status_code, 401)

    def test_token_with_unallowed_algorithm_is_rejected(self):
        user = User(
            id=uuid4(),
            email="demo@example.com",
            password_hash=hash_password("secret"),
            display_name="Demo User",
            role="CAREGIVER",
            is_active=True,
        )
        token = jwt.encode(
            {"sub": str(user.id)},
            "test-only-secret-48-bytes-long-123456789012345678",
            algorithm="HS384",
        )
        with self.assertRaises(HTTPException) as error:
            get_current_user(
                HTTPAuthorizationCredentials(scheme="Bearer", credentials=token),
                FakeSession(user),
            )
        self.assertEqual(error.exception.status_code, 401)

    def test_malformed_and_wrong_signature_tokens_are_rejected(self):
        user = User(
            id=uuid4(),
            email="demo@example.com",
            password_hash=hash_password("secret"),
            display_name="Demo User",
            role="CAREGIVER",
            is_active=True,
        )
        wrong_signature = jwt.encode(
            {"sub": str(user.id)},
            "different-test-secret-32-bytes-long-123456",
            algorithm="HS256",
        )
        unknown_critical_header = jwt.encode(
            {"sub": str(user.id)},
            "test-only-secret-32-bytes-long-123456",
            algorithm="HS256",
            headers={"crit": ["x-unknown-policy"], "x-unknown-policy": "reject-me"},
        )
        for token in ("not-a-jwt", wrong_signature, unknown_critical_header):
            with self.subTest(token=token):
                with self.assertRaises(HTTPException) as error:
                    get_current_user(
                        HTTPAuthorizationCredentials(scheme="Bearer", credentials=token),
                        FakeSession(user),
                    )
                self.assertEqual(error.exception.status_code, 401)

    def test_inactive_user_cannot_login(self):
        user = User(
            id=uuid4(),
            email="inactive@example.com",
            password_hash=hash_password("secret"),
            display_name="Inactive User",
            role="CAREGIVER",
            is_active=False,
        )
        with self.assertRaises(HTTPException) as login_error:
            login(
                type("Credentials", (), {"email": "inactive@example.com", "password": "secret"})(),
                FakeSession(user),
            )
        self.assertEqual(login_error.exception.status_code, 401)
