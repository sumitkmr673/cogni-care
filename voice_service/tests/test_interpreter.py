import unittest

from app.interpreter import NO_MATCH, build_messages, choice_schema, parse_choice
from app.schemas import VoiceContext
from app.transcriber import whisper_language

CONTEXT = VoiceContext(
    language="bn",
    question="Would you like to play a memory game?",
    options=[
        {"id": "GAME_YES", "label": "Yes, let's play"},
        {"id": "GAME_LATER", "label": "Maybe later"},
    ],
)


class PromptTest(unittest.TestCase):
    def test_prompt_lists_every_option_plus_an_explicit_way_out(self):
        user = build_messages("hyan khelbo", CONTEXT)[1]["content"]
        self.assertIn("GAME_YES: Yes, let's play", user)
        self.assertIn("GAME_LATER: Maybe later", user)
        self.assertIn(f"{NO_MATCH}:", user)
        self.assertIn("Would you like to play a memory game?", user)
        self.assertIn('"hyan khelbo"', user)

    def test_thinking_is_switched_off_for_a_fast_single_answer(self):
        self.assertIn("/no_think", build_messages("yes", CONTEXT)[0]["content"])

    def test_output_is_constrained_to_the_offered_ids(self):
        enum = choice_schema(CONTEXT)["properties"]["choice"]["enum"]
        self.assertEqual(["GAME_YES", "GAME_LATER", NO_MATCH], enum)


class ParseChoiceTest(unittest.TestCase):
    def test_a_valid_choice_is_returned(self):
        self.assertEqual("GAME_YES", parse_choice('{"choice": "GAME_YES"}', CONTEXT))

    def test_none_means_not_understood(self):
        self.assertIsNone(parse_choice(f'{{"choice": "{NO_MATCH}"}}', CONTEXT))

    def test_anything_malformed_is_treated_as_not_understood(self):
        for raw in ["", "not json", "[]", '{"choice": "SOMETHING_ELSE"}', '{"other": "GAME_YES"}']:
            with self.subTest(raw=raw):
                self.assertIsNone(parse_choice(raw, CONTEXT))


class WhisperLanguageTest(unittest.TestCase):
    def test_supported_app_languages_are_passed_through(self):
        for tag, expected in [("en", "en"), ("hi", "hi"), ("bn", "bn"), ("as", "as"), ("ne", "ne"), ("hi-IN", "hi")]:
            with self.subTest(tag=tag):
                self.assertEqual(expected, whisper_language(tag))

    def test_languages_whisper_does_not_know_fall_back_to_auto_detect(self):
        for tag in ["mni", "kha", "lus", None, ""]:
            with self.subTest(tag=tag):
                self.assertIsNone(whisper_language(tag))


if __name__ == "__main__":
    unittest.main()
