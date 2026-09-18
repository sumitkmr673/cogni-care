"""CogniCare voice service — the Android voice assistant's own backend.

Deliberately separate from cogni-care/backend: that service is shared by the web portal and the
app, while voice is app-only and needs a GPU, Whisper and a local LLM that the shared backend (and
its Docker image) should not carry. It has no database and never calls the main backend; it only
verifies the app's existing access token with the same JWT secret.
"""

import os
import tempfile
import threading
import time
from contextlib import asynccontextmanager

from fastapi import APIRouter, Depends, FastAPI, File, Form, HTTPException, Request, UploadFile
from pydantic import ValidationError

from app.auth import require_app_user
from app.config import Settings, load_settings
from app.interpreter import Interpreter, QwenInterpreter
from app.schemas import HealthResponse, InterpretResponse, InterpretTextRequest, VoiceContext
from app.transcriber import Transcriber, WhisperTranscriber, whisper_language

# Extensions PyAV (used by faster-whisper) decodes; the app records AAC in .m4a.
AUDIO_SUFFIXES = {".m4a", ".aac", ".mp4", ".3gp", ".wav", ".ogg", ".opus", ".webm", ".mp3", ".flac", ".amr"}

router = APIRouter(prefix="/app/voice", tags=["app voice assistant"])


def create_app(
    settings: Settings | None = None,
    transcriber: Transcriber | None = None,
    interpreter: Interpreter | None = None,
) -> FastAPI:
    """Real models are loaded at startup unless a test passes stand-ins."""

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        resolved = settings or load_settings()
        if not resolved.jwt_secret_key:
            raise RuntimeError(
                "JWT_SECRET_KEY must be set to the same value as the main backend, "
                "or the app's access tokens cannot be verified"
            )
        app.state.settings = resolved
        app.state.transcriber = transcriber or WhisperTranscriber(
            resolved.whisper_model, resolved.whisper_device, resolved.whisper_compute_type
        )
        app.state.interpreter = interpreter or QwenInterpreter(
            resolved.llm_path, resolved.llm_gpu_layers, resolved.llm_context
        )
        # One GPU, two models: requests take turns rather than running out of VRAM together.
        app.state.gpu_lock = threading.Lock()
        yield

    app = FastAPI(title="CogniCare Voice Service", version="0.1.0", lifespan=lifespan)
    app.include_router(router)
    return app


def _checked_choice(choice: str | None, context: VoiceContext) -> str | None:
    """Never hands the app an id it did not offer, whatever the interpreter returned."""
    return choice if choice in {option.id for option in context.options} else None


def _whisper_hint(context: VoiceContext) -> str:
    return ", ".join(option.label for option in context.options)


@router.get("/health", response_model=HealthResponse)
def health(request: Request) -> HealthResponse:
    settings = request.app.state.settings
    return HealthResponse(
        status="ok",
        whisper_model=settings.whisper_model,
        llm_model=settings.llm_path.name,
        device=settings.whisper_device,
    )


@router.post("/interpret", response_model=InterpretResponse)
def interpret_audio(
    request: Request,
    audio: UploadFile = File(..., description="The patient's recorded answer (.m4a, .wav, …)"),
    context: str = Form(..., description="VoiceContext as a JSON string"),
    _user_id: str = Depends(require_app_user),
) -> InterpretResponse:
    """Whisper transcribes the recording, then Qwen picks which offered option it means."""
    try:
        parsed_context = VoiceContext.model_validate_json(context)
    except ValidationError as error:
        # include_context=False: a model_validator's context holds the raised exception object,
        # which is not JSON-serialisable and would turn this 422 into a 500.
        detail = error.errors(include_url=False, include_context=False, include_input=False)
        raise HTTPException(status_code=422, detail=detail) from None

    state = request.app.state
    suffix = os.path.splitext(audio.filename or "")[1].lower()
    temp_path = _save_upload(audio, suffix if suffix in AUDIO_SUFFIXES else ".bin", state.settings.max_audio_bytes)
    try:
        with state.gpu_lock:
            started = time.perf_counter()
            transcript, language = state.transcriber.transcribe(
                temp_path, whisper_language(parsed_context.language), _whisper_hint(parsed_context)
            )
            transcribed = time.perf_counter()
            choice = state.interpreter.choose(transcript, parsed_context)
            finished = time.perf_counter()
    finally:
        os.remove(temp_path)

    return InterpretResponse(
        transcript=transcript,
        choice=_checked_choice(choice, parsed_context),
        language=language,
        timings_ms={
            "transcribe": int((transcribed - started) * 1000),
            "interpret": int((finished - transcribed) * 1000),
        },
    )


@router.post("/interpret-text", response_model=InterpretResponse)
def interpret_text(
    body: InterpretTextRequest,
    request: Request,
    _user_id: str = Depends(require_app_user),
) -> InterpretResponse:
    """For a transcript the phone already has (its own speech recogniser): Qwen only, no Whisper."""
    state = request.app.state
    with state.gpu_lock:
        started = time.perf_counter()
        choice = state.interpreter.choose(body.transcript, body.context)
        finished = time.perf_counter()
    return InterpretResponse(
        transcript=body.transcript,
        choice=_checked_choice(choice, body.context),
        language=None,
        timings_ms={"interpret": int((finished - started) * 1000)},
    )


def _save_upload(upload: UploadFile, suffix: str, max_bytes: int) -> str:
    """Streams the upload to a temp file, refusing anything larger than a short spoken answer."""
    handle = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    written = 0
    try:
        with handle:
            while chunk := upload.file.read(64 * 1024):
                written += len(chunk)
                if written > max_bytes:
                    raise HTTPException(
                        status_code=413,
                        detail=f"Recording is larger than {max_bytes} bytes",
                    )
                handle.write(chunk)
        if written == 0:
            raise HTTPException(status_code=422, detail="Recording is empty")
    except BaseException:
        os.remove(handle.name)
        raise
    return handle.name


app = create_app()
