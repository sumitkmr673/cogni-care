import os
import unittest
from datetime import timedelta
from uuid import uuid4

os.environ.setdefault("JWT_SECRET_KEY", "test-only-secret")

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
