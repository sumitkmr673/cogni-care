import io
import json
import unittest
import urllib.error
from unittest.mock import patch

from fastapi.testclient import TestClient

from app.interpreter import InterpreterUnavailable, LlamaServerInterpreter, NO_MATCH, request_body
from app.main import create_app
from app.schemas import VoiceContext
from tests.test_api import FakeTranscriber, MEDICATION_CONTEXT, make_settings, make_token

CONTEXT = VoiceContext.model_validate(MEDICATION_CONTEXT)


class FakeResponse(io.BytesIO):
    status = 200

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False


def completion(content: str) -> FakeResponse:
    return FakeResponse(json.dumps({"choices": [{"message": {"content": content}}]}).encode())


class RequestBodyTest(unittest.TestCase):
    def test_output_is_grammar_constrained_to_the_offered_ids(self):
        body = request_body("yes", CONTEXT)
        enum = body["response_format"]["schema"]["properties"]["choice"]["enum"]
        self.assertEqual(["MEDICATION_TAKEN", "MEDICATION_NOT_YET", NO_MATCH], enum)

    def test_thinking_is_switched_off_and_decoding_is_deterministic(self):
        body = request_body("yes", CONTEXT)
        self.assertEqual({"enable_thinking": False}, body["chat_template_kwargs"])
        self.assertEqual(0.0, body["temperature"])


class LlamaServerInterpreterTest(unittest.TestCase):
    def setUp(self):
        self.interpreter = LlamaServerInterpreter("http://127.0.0.1:8101")

    def test_the_models_choice_is_returned(self):
        with patch("urllib.request.urlopen", return_value=completion('{"choice": "MEDICATION_TAKEN"}')) as urlopen:
            self.assertEqual("MEDICATION_TAKEN", self.interpreter.choose("haan le li", CONTEXT))
        request = urlopen.call_args.args[0]
        self.assertEqual("http://127.0.0.1:8101/v1/chat/completions", request.full_url)

    def test_none_from_the_model_means_not_understood(self):
        with patch("urllib.request.urlopen", return_value=completion(f'{{"choice": "{NO_MATCH}"}}')):
            self.assertIsNone(self.interpreter.choose("hmm", CONTEXT))

    def test_silence_never_reaches_the_model(self):
        with patch("urllib.request.urlopen") as urlopen:
            self.assertIsNone(self.interpreter.choose("   ", CONTEXT))
        urlopen.assert_not_called()

    def test_an_unreachable_server_is_reported_not_guessed(self):
        with patch("urllib.request.urlopen", side_effect=urllib.error.URLError("refused")):
            with self.assertRaises(InterpreterUnavailable):
                self.interpreter.choose("yes", CONTEXT)

    def test_is_ready_is_false_when_nothing_is_listening(self):
        self.assertFalse(LlamaServerInterpreter("http://127.0.0.1:1").is_ready())


class DownInterpreter:
    def choose(self, transcript, context):
        raise InterpreterUnavailable("down")

    def is_ready(self):
        return False


class ModelDownApiTest(unittest.TestCase):
    def setUp(self):
        client = TestClient(create_app(make_settings(), FakeTranscriber(), DownInterpreter()))
        client.__enter__()
        self.addCleanup(client.__exit__, None, None, None)
        self.client = client

    def test_the_app_gets_503_so_it_can_fall_back_on_device(self):
        response = self.client.post(
            "/app/voice/interpret-text",
            headers={"Authorization": f"Bearer {make_token()}"},
            json={"transcript": "yes", "context": MEDICATION_CONTEXT},
        )
        self.assertEqual(503, response.status_code)

    def test_health_says_degraded(self):
        body = self.client.get("/app/voice/health").json()
        self.assertEqual("degraded", body["status"])
        self.assertFalse(body["llm_ready"])


if __name__ == "__main__":
    unittest.main()
