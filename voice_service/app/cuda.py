import os
import sys
from pathlib import Path

_registered = False


def register_cuda_dlls() -> None:
    """Makes pip-installed CUDA 12 libraries findable on Windows.

    Both models need them on the GPU: faster-whisper (cuBLAS, cuDNN) and the prebuilt CUDA
    llama-cpp-python wheel (CUDA runtime, cuBLAS). The nvidia-*-cu12 wheels put the DLLs under
    site-packages/nvidia/*/bin, which Windows does not search by default. Safe to call repeatedly.
    """
    global _registered
    if _registered or sys.platform != "win32":
        return
    for site in map(Path, sys.path):
        nvidia = site / "nvidia"
        if not nvidia.is_dir():
            continue
        for bin_dir in nvidia.glob("*/bin"):
            os.add_dll_directory(str(bin_dir))
            os.environ["PATH"] = str(bin_dir) + os.pathsep + os.environ.get("PATH", "")
    _registered = True
