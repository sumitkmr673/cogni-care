from pydantic import BaseModel, Field, model_validator


class VoiceOption(BaseModel):
    """One answer the patient could mean. `id` is what the app gets back; `label` is shown to
    the model and used to bias Whisper towards the words the patient is likely to say."""

    id: str = Field(min_length=1, max_length=64, pattern=r"^[A-Za-z0-9_\-]+$")
    label: str = Field(min_length=1, max_length=120)


class VoiceContext(BaseModel):
    # The app's language tag ("en", "hi", "bn", …); used as Whisper's language hint.
    language: str | None = Field(default=None, max_length=16)
    # The question on screen, or null for the free-form assistant.
    question: str | None = Field(default=None, max_length=300)
    options: list[VoiceOption] = Field(min_length=1, max_length=6)

    @model_validator(mode="after")
    def option_ids_are_unique(self) -> "VoiceContext":
        ids = [option.id for option in self.options]
        if len(ids) != len(set(ids)):
            raise ValueError("option ids must be unique")
        return self


class InterpretTextRequest(BaseModel):
    transcript: str = Field(max_length=500)
    context: VoiceContext


class InterpretResponse(BaseModel):
    transcript: str
    # One of the option ids sent in the request, or null when the answer was unclear —
    # the app then asks again gently or lets the patient tap.
    choice: str | None
    # Language Whisper transcribed in (null for /interpret-text, which receives text).
    language: str | None
    timings_ms: dict[str, int]


class HealthResponse(BaseModel):
    status: str
    whisper_model: str
    llm_model: str
    device: str
