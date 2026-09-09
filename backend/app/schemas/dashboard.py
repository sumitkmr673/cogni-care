from datetime import date, datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict


class PatientSummary(BaseModel):
    id: UUID
    display_name: str
    preferred_language: str | None
    timezone: str | None
    profile_photo_ref: str | None


class PatientProfile(PatientSummary):
    date_of_birth: date | None
    gender: str | None


class CaregiverRelationship(BaseModel):
    caregiver_id: UUID
    display_name: str
    caregiver_type: str
    is_primary: bool


class SessionResult(BaseModel):
    score: float
    accuracy: float | None
    correct_answers: int | None
    total_questions: int | None
    response_time_ms: int | None
    mistakes: int | None


class RecentGameSession(BaseModel):
    id: UUID
    game_id: UUID
    game_code: str
    game_name: str
    started_at: datetime
    completed_at: datetime | None
    status: str
    difficulty_level: int
    result: SessionResult | None


class PerformancePoint(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    metric_date: date
    memory_score: float | None
    attention_score: float | None
    average_accuracy: float | None
    average_response_time_ms: int | None
    games_completed: int
    total_sessions: int


class ReminderItem(BaseModel):
    id: UUID
    title: str
    description: str | None
    reminder_type: str
    scheduled_at: datetime
    is_recurring: bool
    recurrence_rule: str | None


class PatientsResponse(BaseModel):
    patients: list[PatientSummary]


class DashboardResponse(BaseModel):
    patient: PatientProfile
    caregiver_relationship: CaregiverRelationship | None
    recent_sessions: list[RecentGameSession]
    latest_performance: PerformancePoint | None
    active_reminders: list[ReminderItem]


class PerformanceHistoryResponse(BaseModel):
    patient_id: UUID
    metrics: list[PerformancePoint]
