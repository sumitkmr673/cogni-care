import os
from dataclasses import dataclass
from pathlib import Path

SERVICE_ROOT = Path(__file__).resolve().parent.parent
REPO_ROOT = SERVICE_ROOT.parent


@dataclass(frozen=True)
class Settings:
    # Same values as the main backend's, so the app's existing access token works here unchanged.
    jwt_secret_key: str
    jwt_algorithm: str

    # The GGUF file run.ps1 hands to llama-server, and where that server listens.
    llm_path: Path
    llm_url: str

    whisper_model: str
    whisper_device: str
    whisper_compute_type: str

    max_audio_bytes: int


def load_settings() -> Settings:
    return Settings(
        jwt_secret_key=os.environ.get("JWT_SECRET_KEY", ""),
        jwt_algorithm=os.environ.get("JWT_ALGORITHM", "HS256"),
        llm_path=Path(
            os.environ.get("VOICE_LLM_PATH", str(REPO_ROOT / "Models" / "Qwen3-8B-Q4_K_M.gguf"))
        ),
        llm_url=os.environ.get("VOICE_LLM_URL", "http://127.0.0.1:8101"),
        whisper_model=os.environ.get("VOICE_WHISPER_MODEL", "medium"),
        whisper_device=os.environ.get("VOICE_WHISPER_DEVICE", "cuda"),
        # int8_float16 keeps Whisper medium near 1 GB of VRAM, leaving room for Qwen3-8B on an
        # 8 GB card; accuracy is practically the same as float16 for short answers.
        whisper_compute_type=os.environ.get("VOICE_WHISPER_COMPUTE_TYPE", "int8_float16"),
        max_audio_bytes=int(os.environ.get("VOICE_MAX_AUDIO_BYTES", str(5 * 1024 * 1024))),
    )
