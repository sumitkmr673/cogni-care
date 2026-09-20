import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from app.api.auth import router as auth_router
from app.api.dashboard import router as dashboard_router
from app.api.care_team import router as care_team_router
from app.api.gameplay import router as gameplay_router
from app.db.session import engine


def _get_cors_origins() -> list[str]:
    """Parse configured CORS origins while preserving local development defaults."""
    default_origins = [
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:3000",
        "http://127.0.0.1:3000",
    ]
    raw = os.environ.get("CORS_ALLOWED_ORIGINS", "")
    configured = [
        origin.strip().rstrip("/")
        for origin in raw.split(",")
        if origin.strip()
    ]
    seen = set()
    result = []
    for origin in configured + default_origins:
        cleaned = origin.rstrip("/")
        if cleaned and cleaned not in seen:
            seen.add(cleaned)
            result.append(cleaned)
    return result


app = FastAPI(
    title="SIH2026 Backend",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=_get_cors_origins(),
    allow_credentials=True,
    allow_methods=["GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"],
)

app.include_router(dashboard_router)
app.include_router(care_team_router)
app.include_router(auth_router)
app.include_router(gameplay_router)


@app.get("/")
async def root():
    return {"message": "SIH2026 backend is running"}


@app.get("/health/db")
async def database_health():
    with engine.connect() as connection:
        result = connection.execute(text("SELECT 1"))
        value = result.scalar()

    return {
        "database": "connected",
        "result": value,
    }
