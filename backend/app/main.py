from fastapi import FastAPI
from sqlalchemy import text

from app.api.dashboard import router as dashboard_router
from app.db.session import engine

app = FastAPI(
    title="SIH2026 Backend",
    version="0.1.0",
)
app.include_router(dashboard_router)


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
