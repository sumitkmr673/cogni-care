import json
import urllib.error
import urllib.request
from typing import Protocol

from app.schemas import VoiceContext

# The model's way of saying "I'm not sure". Never returned to the app; it becomes `choice: null`.
NO_MATCH = "NONE"

SYSTEM_PROMPT = (
    "You interpret a short spoken answer from an elderly person using a memory-care app. "
    "They may speak English, Hindi, Bengali, Assamese or Nepali, or mix them. "
    "Choose the one option that matches what they meant. "
    f"If the answer is unclear, unrelated, or says two different things, choose {NO_MATCH} — "
    "never guess, because a wrong guess could record something untrue about their care. "
    "An answer that starts with no and then says yes (or the other way round), such as "
    f'"no, I took it" or "yes, not yet", contradicts itself: choose {NO_MATCH}. '
    "Reply with JSON only. /no_think"
)


def build_messages(transcript: str, context: VoiceContext) -> list[dict[str, str]]:
    lines: list[str] = []
    if context.question:
        lines.append(f"Question on screen: {context.question}")
    lines.append("Options:")
    lines.extend(f"- {option.id}: {option.label}" for option in context.options)
    lines.append(f"- {NO_MATCH}: unclear, or none of the above")
    # Quoted and length-capped by the schema; the output is constrained to the option ids below,
    # so nothing said here can make the model return anything else.
    lines.append(f'What they said: "{transcript}"')
    return [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": "\n".join(lines)},
    ]


def choice_schema(context: VoiceContext) -> dict:
    return {
        "type": "object",
        "properties": {
            "choice": {"type": "string", "enum": [option.id for option in context.options] + [NO_MATCH]},
        },
        "required": ["choice"],
    }


def parse_choice(raw: str, context: VoiceContext) -> str | None:
    """The model's reply as an option id, or None for NONE, malformed JSON, or an unknown id."""
    try:
        value = json.loads(raw).get("choice")
    except (ValueError, AttributeError):
        return None
    valid_ids = {option.id for option in context.options}
    return value if value in valid_ids else None


class InterpreterUnavailable(RuntimeError):
    """The language model could not be reached — distinct from an answer it did not understand."""


class Interpreter(Protocol):
    def choose(self, transcript: str, context: VoiceContext) -> str | None:
        ...


def request_body(transcript: str, context: VoiceContext) -> dict:
    return {
        "messages": build_messages(transcript, context),
        # Grammar-constrained: the model can only emit {"choice": "<one of the ids>"}.
        "response_format": {"type": "json_object", "schema": choice_schema(context)},
        # Qwen3 thinks aloud by default; for picking one option that only adds latency.
        "chat_template_kwargs": {"enable_thinking": False},
        "temperature": 0.0,
        "max_tokens": 32,
    }


class LlamaServerInterpreter:
    """Qwen3 served by llama.cpp's own `llama-server` on this machine (started by run.ps1).

    Qwen runs in llama.cpp's official build rather than inside this Python process: the prebuilt
    llama-cpp-python CUDA wheel is compiled with AVX-512 and crashes on CPUs without it (e.g. the
    Core Ultra 7 155H), whereas the official build picks a CPU variant at runtime.
    """

    def __init__(self, base_url: str, timeout_s: float = 30.0):
        self._base_url = base_url.rstrip("/")
        self._timeout_s = timeout_s

    def is_ready(self) -> bool:
        try:
            with urllib.request.urlopen(f"{self._base_url}/health", timeout=2) as response:
                return response.status == 200
        except (urllib.error.URLError, TimeoutError, OSError):
            return False

    def choose(self, transcript: str, context: VoiceContext) -> str | None:
        if not transcript.strip():
            return None
        request = urllib.request.Request(
            f"{self._base_url}/v1/chat/completions",
            data=json.dumps(request_body(transcript, context)).encode("utf-8"),
            headers={"Content-Type": "application/json"},
        )
        try:
            with urllib.request.urlopen(request, timeout=self._timeout_s) as response:
                payload = json.load(response)
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            raise InterpreterUnavailable(f"llama-server at {self._base_url} is not reachable") from error
        message = (payload.get("choices") or [{}])[0].get("message") or {}
        return parse_choice(message.get("content") or "", context)
