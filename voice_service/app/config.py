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

    llm_path: Path
    # -1 offloads every layer to the GPU; lower it if the model does not fit in VRAM.
    llm_gpu_layers: int
    llm_context: int

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
        llm_gpu_layers=int(os.environ.get("VOICE_LLM_GPU_LAYERS", "-1")),
        llm_context=int(os.environ.get("VOICE_LLM_CONTEXT", "2048")),
        whisper_model=os.environ.get("VOICE_WHISPER_MODEL", "medium"),
        whisper_device=os.environ.get("VOICE_WHISPER_DEVICE", "cuda"),
        whisper_compute_type=os.environ.get("VOICE_WHISPER_COMPUTE_TYPE", "float16"),
        max_audio_bytes=int(os.environ.get("VOICE_MAX_AUDIO_BYTES", str(5 * 1024 * 1024))),
    )
