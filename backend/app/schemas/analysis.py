from datetime import date, datetime
from uuid import UUID

from pydantic import BaseModel, Field


class EvidenceData(BaseModel):
    """Numerical evidence supporting a performance observation."""
    recent_average: float | None = None
    baseline_average: float | None = None
    delta: float | None = None
    unit: str = Field(description="Unit of measurement, e.g. '%', 'points', 'ms'")


class PerformanceObservation(BaseModel):
    """A deterministic, non-clinical observation derived from gameplay data."""
    category: str = Field(
        description="Category: 'ACCURACY', 'MEMORY', 'ATTENTION', 'RESPONSE_TIME', 'ENGAGEMENT', 'DATA_SUFFICIENCY'"
    )
    direction: str = Field(
        description="Observed trend: 'IMPROVING', 'STABLE', 'LOWER', 'FASTER', 'SLOWER', 'INSUFFICIENT_DATA', 'NOT_ENOUGH_DATA'"
    )
    severity: str = Field(
        description="Visual indicator: 'POSITIVE', 'INFO', 'ATTENTION'"
    )
    title: str = Field(description="Short human-readable title for the observation")
    message: str = Field(description="Clear, non-clinical explanatory message")
    evidence: EvidenceData | None = Field(
        default=None, description="Quantitative delta and averages if available"
    )


class DataWindowInfo(BaseModel):
    """Metadata describing the time window and active gameplay days evaluated."""
    total_active_days: int
    recent_days_count: int
    baseline_days_count: int
    recent_start_date: date | None = None
    recent_end_date: date | None = None
    baseline_start_date: date | None = None
    baseline_end_date: date | None = None
    recent_games_completed: int


class AnalysisSummary(BaseModel):
    """High-level summary of the analysis for quick caregiver review."""
    headline: str
    primary_direction: str = Field(
        description="'IMPROVING', 'STABLE', 'LOWER', 'VARIABLE', 'INSUFFICIENT_DATA'"
    )


class PatientAnalysisResponse(BaseModel):
    """Top-level response model for the patient performance analysis endpoint."""
    patient_id: UUID
    generated_at: datetime
    data_sufficiency: str = Field(description="'SUFFICIENT' or 'INSUFFICIENT'")
    data_window: DataWindowInfo
    summary: AnalysisSummary
    observations: list[PerformanceObservation]
