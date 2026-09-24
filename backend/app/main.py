import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

import app.models  # noqa: F401
from app.api.auth import router as auth_router
from app.api.dashboard import router as dashboard_router
from app.api.care_team import router as care_team_router
from app.api.gameplay import router as gameplay_router
from app.db.session import Base, engine


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


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Ensure all tables exist on startup
    try:
        Base.metadata.create_all(bind=engine)
        print("INFO: Base.metadata.create_all completed.")
    except Exception as exc:
        print(f"ERROR Base.metadata.create_all: {exc}")

    try:
        from alembic.config import Config
        from alembic import command
        alembic_cfg = Config("alembic.ini")
        command.upgrade(alembic_cfg, "head")
        print("INFO: alembic upgrade head completed successfully.")
    except Exception as exc:
        print(f"INFO alembic migration notice: {exc}")

    yield


app = FastAPI(
    title="SIH2026 Backend",
    version="0.1.0",
    lifespan=lifespan,
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
        tables_res = connection.execute(
            text("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename")
        )
        tables = [r[0] for r in tables_res.fetchall()]

        alembic_versions = []
        try:
            alembic_res = connection.execute(text("SELECT version_num FROM alembic_version"))
            alembic_versions = [r[0] for r in alembic_res.fetchall()]
        except Exception:
            pass

    return {
        "database": "connected",
        "result": value,
        "tables": tables,
        "alembic_versions": alembic_versions,
        "version": "v1.0.4-tables",
    }

