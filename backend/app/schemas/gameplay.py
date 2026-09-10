from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator


class GameSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    code: str
    name: str
    category: str
    description: str | None


class GamesResponse(BaseModel):
    games: list[GameSummary]


class StartGameSessionRequest(BaseModel):
    difficulty_level: int = Field(default=1, ge=1, le=3)


class GameSessionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    game_id: UUID
    patient_id: UUID
    difficulty_level: int
    status: str
    started_at: datetime
    completed_at: datetime | None


class SubmitGameResultRequest(BaseModel):
    score: Decimal = Field(ge=0)
    accuracy: Decimal | None = Field(default=None, ge=0, le=100)
    correct_answers: int | None = Field(default=None, ge=0)
    total_questions: int | None = Field(default=None, gt=0)
    response_time_ms: int | None = Field(default=None, ge=0)
    mistakes: int | None = Field(default=None, ge=0)

    @model_validator(mode="after")
    def validate_correct_answers(self) -> "SubmitGameResultRequest":
        if (
            self.correct_answers is not None
            and self.total_questions is not None
            and self.correct_answers > self.total_questions
        ):
            raise ValueError("correct_answers cannot exceed total_questions")
        return self


class GameResultResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    session_id: UUID
    score: Decimal
    accuracy: Decimal | None
    correct_answers: int | None
    total_questions: int | None
    response_time_ms: int | None
    mistakes: int | None
    session_status: str
    completed_at: datetime | None
