import unittest

from fastapi.testclient import TestClient

from app.main import MAX_HINT_CHARS, create_app
from tests.test_api import FakeInterpreter, FakeTranscriber, make_settings, make_token


class TranscribeApiTest(unittest.TestCase):
    def setUp(self):
        self.transcriber = FakeTranscriber(text="सेब", language="hi")
        self.interpreter = FakeInterpreter()
        client = TestClient(create_app(make_settings(), self.transcriber, self.interpreter))
        client.__enter__()
        self.addCleanup(client.__exit__, None, None, None)
        self.client = client

    def post(self, headers=None, **form):
        return self.client.post(
            "/app/voice/transcribe",
            headers={"Authorization": f"Bearer {make_token()}"} if headers is None else headers,
            files={"audio": ("answer.wav", b"RIFF-fake-audio", "audio/wav")},
            data=form,
        )

    def test_returns_whispers_transcript_without_calling_qwen(self):
        response = self.post(language="hi", hint="सेब, केला, आम")
        self.assertEqual(200, response.status_code)
        self.assertEqual("सेब", response.json()["transcript"])
        self.assertEqual("hi", response.json()["language"])
        self.assertIn("transcribe", response.json()["timings_ms"])
        self.assertEqual([], self.interpreter.calls)

    def test_language_and_hint_reach_whisper(self):
        self.post(language="bn-IN", hint="আপেল, কলা")
        call = self.transcriber.calls[0]
        self.assertEqual("bn", call["language"])
        self.assertEqual("আপেল, কলা", call["hint"])

    def test_no_language_means_auto_detect_and_no_hint_means_none(self):
        self.post()
        call = self.transcriber.calls[0]
        self.assertIsNone(call["language"])
        self.assertIsNone(call["hint"])

    def test_a_long_hint_is_cut_short(self):
        self.post(hint="x" * (MAX_HINT_CHARS + 50))
        self.assertEqual(MAX_HINT_CHARS, len(self.transcriber.calls[0]["hint"]))

    def test_requires_a_token(self):
        self.assertEqual(401, self.post(headers={}).status_code)

    def test_an_empty_recording_is_refused(self):
        response = self.client.post(
            "/app/voice/transcribe",
            headers={"Authorization": f"Bearer {make_token()}"},
            files={"audio": ("answer.wav", b"", "audio/wav")},
        )
        self.assertEqual(422, response.status_code)


if __name__ == "__main__":
    unittest.main()
