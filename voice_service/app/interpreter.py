import json
from pathlib import Path
from typing import Protocol

from app.cuda import register_cuda_dlls
from app.schemas import VoiceContext

# The model's way of saying "I'm not sure". Never returned to the app; it becomes `choice: null`.
NO_MATCH = "NONE"

SYSTEM_PROMPT = (
    "You interpret a short spoken answer from an elderly person using a memory-care app. "
    "They may speak English, Hindi, Bengali, Assamese or Nepali, or mix them. "
    "Choose the one option that matches what they meant. "
    f"If the answer is unclear, unrelated, or says two different things, choose {NO_MATCH} — "
    "never guess, because a wrong guess could record something untrue about their care. "
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


class Interpreter(Protocol):
    def choose(self, transcript: str, context: VoiceContext) -> str | None:
        ...


class QwenInterpreter:
    """Qwen3 GGUF through llama-cpp-python. Imported lazily so tests run without the library."""

    def __init__(self, model_path: Path, gpu_layers: int, context_size: int):
        if gpu_layers != 0:
            register_cuda_dlls()
        from llama_cpp import Llama

        if not model_path.is_file():
            raise FileNotFoundError(f"Qwen model not found at {model_path} (set VOICE_LLM_PATH)")
        self._llm = Llama(
            model_path=str(model_path),
            n_gpu_layers=gpu_layers,
            n_ctx=context_size,
            # Qwen's native chat format. Set explicitly rather than read from the GGUF, whose
            # Jinja template uses features llama-cpp-python's renderer does not always support.
            chat_format="chatml",
            verbose=False,
        )

    def choose(self, transcript: str, context: VoiceContext) -> str | None:
        if not transcript.strip():
            return None
        result = self._llm.create_chat_completion(
            messages=build_messages(transcript, context),
            # Grammar-constrained: the model can only emit {"choice": "<one of the ids>"}.
            response_format={"type": "json_object", "schema": choice_schema(context)},
            temperature=0.0,
            max_tokens=32,
        )
        return parse_choice(result["choices"][0]["message"]["content"] or "", context)
