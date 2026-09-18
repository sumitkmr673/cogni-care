from typing import Protocol

from app.cuda import register_cuda_dlls

# App languages Whisper can transcribe. Manipuri (mni), Khasi (kha) and Mizo (lus) are not in
# Whisper's language list, so for those it auto-detects rather than being forced into one.
WHISPER_LANGUAGES = {"en", "hi", "bn", "as", "ne"}


def whisper_language(app_language_tag: str | None) -> str | None:
    if not app_language_tag:
        return None
    base = app_language_tag.split("-")[0].lower()
    return base if base in WHISPER_LANGUAGES else None


class Transcriber(Protocol):
    def transcribe(self, audio_path: str, language: str | None, hint: str | None) -> tuple[str, str | None]:
        """Returns (transcript, language Whisper used)."""
        ...


class WhisperTranscriber:
    """faster-whisper (CTranslate2). Imported lazily so tests run without the library."""

    def __init__(self, model_name: str, device: str, compute_type: str):
        if device == "cuda":
            register_cuda_dlls()
        from faster_whisper import WhisperModel

        self._model = WhisperModel(model_name, device=device, compute_type=compute_type)

    def transcribe(self, audio_path: str, language: str | None, hint: str | None) -> tuple[str, str | None]:
        segments, info = self._model.transcribe(
            audio_path,
            language=language,
            beam_size=5,
            # Trims the silence around a short answer, which is where Whisper tends to invent words.
            vad_filter=True,
            # The option labels, so "haan" or "khelna" are heard as the words the app is expecting.
            initial_prompt=hint,
            condition_on_previous_text=False,
        )
        text = " ".join(segment.text.strip() for segment in segments).strip()
        return text, info.language
