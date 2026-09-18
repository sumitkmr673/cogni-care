import json
import os
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import uuid4

import jwt
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app

TEST_SECRET = "test-only-secret-32-bytes-long-123456"

MEDICATION_CONTEXT = {
    "language": "hi",
    "question": "Did you take your medicine?",
    "options": [
        {"id": "MEDICATION_TAKEN", "label": "Yes, I took it"},
        {"id": "MEDICATION_NOT_YET", "label": "Not yet"},
    ],
}


def make_settings(**overrides) -> Settings:
    values = dict(
        jwt_secret_key=TEST_SECRET,
        jwt_algorithm="HS256",
        llm_path=Path("unused.gguf"),
        llm_gpu_layers=0,
        llm_context=512,
        whisper_model="medium",
        whisper_device="cpu",
        whisper_compute_type="int8",
        max_audio_bytes=1024,
    )
    values.update(overrides)
    return Settings(**values)


def make_token(secret: str = TEST_SECRET, expires_in: timedelta = timedelta(minutes=10)) -> str:
    # Same claims the main backend's create_access_token issues.
    now = datetime.now(timezone.utc)
    return jwt.encode({"sub": str(uuid4()), "iat": now, "exp": now + expires_in}, secret, algorithm="HS256")


class FakeTranscriber:
    def __init__(self, text: str = "haan, le li", language: str = "hi"):
        self.text = text
        self.language = language
        self.calls: list[dict] = []

    def transcribe(self, audio_path, language, hint):
        with open(audio_path, "rb") as audio:
            self.calls.append({"path": audio_path, "language": language, "hint": hint, "bytes": audio.read()})
        return self.text, self.language


class FakeInterpreter:
    def __init__(self, choice: str | None = "MEDICATION_TAKEN"):
        self.choice = choice
        self.calls: list[tuple[str, object]] = []

    def choose(self, transcript, context):
        self.calls.append((transcript, context))
        return self.choice


class VoiceApiTest(unittest.TestCase):
    def client(self, transcriber=None, interpreter=None, **settings) -> TestClient:
        self.transcriber = transcriber or FakeTranscriber()
        self.interpreter = interpreter or FakeInterpreter()
        app = create_app(make_settings(**settings), self.transcriber, self.interpreter)
        client = TestClient(app)
        client.__enter__()  # runs the lifespan
        self.addCleanup(client.__exit__, None, None, None)
        return client

    def auth(self, token: str | None = None) -> dict[str, str]:
        return {"Authorization": f"Bearer {token or make_token()}"}

    def post_audio(self, client, audio: bytes = b"fake-m4a-bytes", context=None, filename="answer.m4a", headers=None):
        return client.post(
            "/app/voice/interpret",
            headers=self.auth() if headers is None else headers,
            files={"audio": (filename, audio, "audio/mp4")},
            data={"context": json.dumps(context or MEDICATION_CONTEXT)},
        )

    # ---- health / startup ----------------------------------------------------------------

    def test_health_reports_the_configured_models(self):
        response = self.client().get("/app/voice/health")
        self.assertEqual(200, response.status_code)
        self.assertEqual("ok", response.json()["status"])
        self.assertEqual("medium", response.json()["whisper_model"])

    def test_refuses_to_start_without_the_shared_jwt_secret(self):
        app = create_app(make_settings(jwt_secret_key=""), FakeTranscriber(), FakeInterpreter())
        with self.assertRaises(RuntimeError):
            with TestClient(app):
                pass

    # ---- auth ------------------------------------------------------------------------------

    def test_requests_without_a_token_are_rejected(self):
        response = self.client().post("/app/voice/interpret-text", json={"transcript": "yes", "context": MEDICATION_CONTEXT})
        self.assertEqual(401, response.status_code)

    def test_expired_tokens_are_rejected(self):
        client = self.client()
        response = client.post(
            "/app/voice/interpret-text",
            headers=self.auth(make_token(expires_in=timedelta(minutes=-1))),
            json={"transcript": "yes", "context": MEDICATION_CONTEXT},
        )
        self.assertEqual(401, response.status_code)

    def test_tokens_signed_with_another_secret_are_rejected(self):
        client = self.client()
        response = client.post(
            "/app/voice/interpret-text",
            headers=self.auth(make_token(secret="some-other-secret-that-is-long-enough")),
            json={"transcript": "yes", "context": MEDICATION_CONTEXT},
        )
        self.assertEqual(401, response.status_code)

    def test_audio_endpoint_also_requires_a_token(self):
        response = self.post_audio(self.client(), headers={})
        self.assertEqual(401, response.status_code)
        self.assertEqual([], self.transcriber.calls)

    # ---- /interpret-text ----------------------------------------------------------------------

    def test_text_is_interpreted_without_whisper(self):
        client = self.client()
        response = client.post(
            "/app/voice/interpret-text",
            headers=self.auth(),
            json={"transcript": "yes I took it", "context": MEDICATION_CONTEXT},
        )
        self.assertEqual(200, response.status_code)
        body = response.json()
        self.assertEqual("MEDICATION_TAKEN", body["choice"])
        self.assertEqual("yes I took it", body["transcript"])
        self.assertIsNone(body["language"])
        self.assertEqual([], self.transcriber.calls)

    def test_an_unclear_answer_comes_back_as_null(self):
        client = self.client(interpreter=FakeInterpreter(choice=None))
        response = client.post(
            "/app/voice/interpret-text",
            headers=self.auth(),
            json={"transcript": "hmm", "context": MEDICATION_CONTEXT},
        )
        self.assertIsNone(response.json()["choice"])

    def test_an_id_the_app_never_offered_is_never_returned(self):
        client = self.client(interpreter=FakeInterpreter(choice="DELETE_ALL_REMINDERS"))
        response = client.post(
            "/app/voice/interpret-text",
            headers=self.auth(),
            json={"transcript": "yes", "context": MEDICATION_CONTEXT},
        )
        self.assertEqual(200, response.status_code)
        self.assertIsNone(response.json()["choice"])

    # ---- /interpret (audio) --------------------------------------------------------------------

    def test_audio_is_transcribed_then_interpreted(self):
        client = self.client()
        response = self.post_audio(client)
        self.assertEqual(200, response.status_code)
        body = response.json()
        self.assertEqual("haan, le li", body["transcript"])
        self.assertEqual("MEDICATION_TAKEN", body["choice"])
        self.assertEqual("hi", body["language"])
        self.assertIn("transcribe", body["timings_ms"])

        call = self.transcriber.calls[0]
        self.assertEqual(b"fake-m4a-bytes", call["bytes"])
        self.assertEqual("hi", call["language"])
        # Option labels bias Whisper towards the words the patient is likely to use.
        self.assertIn("Yes, I took it", call["hint"])
        self.assertEqual("haan, le li", self.interpreter.calls[0][0])

    def test_the_recording_is_deleted_after_use(self):
        client = self.client()
        self.post_audio(client)
        self.assertFalse(os.path.exists(self.transcriber.calls[0]["path"]))

    def test_languages_whisper_lacks_are_auto_detected(self):
        client = self.client()
        self.post_audio(client, context={**MEDICATION_CONTEXT, "language": "mni"})
        self.assertIsNone(self.transcriber.calls[0]["language"])

    def test_recordings_over_the_size_limit_are_refused(self):
        client = self.client(max_audio_bytes=10)
        response = self.post_audio(client, audio=b"x" * 11)
        self.assertEqual(413, response.status_code)
        self.assertEqual([], self.transcriber.calls)

    def test_an_empty_recording_is_refused(self):
        response = self.post_audio(self.client(), audio=b"")
        self.assertEqual(422, response.status_code)

    def test_malformed_context_is_refused(self):
        client = self.client()
        response = client.post(
            "/app/voice/interpret",
            headers=self.auth(),
            files={"audio": ("a.m4a", b"abc", "audio/mp4")},
            data={"context": "{not json"},
        )
        self.assertEqual(422, response.status_code)

    def test_duplicate_option_ids_are_refused(self):
        context = {**MEDICATION_CONTEXT, "options": [{"id": "A", "label": "Yes"}, {"id": "A", "label": "No"}]}
        response = self.post_audio(self.client(), context=context)
        self.assertEqual(422, response.status_code)


if __name__ == "__main__":
    unittest.main()
