import uuid
from datetime import datetime
from decimal import Decimal
from typing import TYPE_CHECKING

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, Integer, Numeric, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base

if TYPE_CHECKING:
    from app.models.game_session import GameSession


class GameResult(Base):
    __tablename__ = "game_results"
    __table_args__ = (
        UniqueConstraint("session_id", name="uq_game_results_session_id"),
        CheckConstraint("score >= 0", name="ck_game_results_score_nonnegative"),
        CheckConstraint("accuracy IS NULL OR (accuracy >= 0 AND accuracy <= 100)", name="ck_game_results_accuracy"),
        CheckConstraint("correct_answers IS NULL OR correct_answers >= 0", name="ck_game_results_correct_nonnegative"),
        CheckConstraint("total_questions IS NULL OR total_questions > 0", name="ck_game_results_total_positive"),
        CheckConstraint(
            "correct_answers IS NULL OR total_questions IS NULL OR correct_answers <= total_questions",
            name="ck_game_results_correct_lte_total",
        ),
        CheckConstraint(
            "response_time_ms IS NULL OR response_time_ms >= 0",
            name="ck_game_results_response_nonnegative",
        ),
        CheckConstraint("mistakes IS NULL OR mistakes >= 0", name="ck_game_results_mistakes_nonnegative"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    session_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("game_sessions.id"), nullable=False
    )
    score: Mapped[Decimal] = mapped_column(Numeric(6, 2), nullable=False)
    accuracy: Mapped[Decimal | None] = mapped_column(Numeric(5, 2))
    correct_answers: Mapped[int | None] = mapped_column(Integer)
    total_questions: Mapped[int | None] = mapped_column(Integer)
    response_time_ms: Mapped[int | None] = mapped_column(Integer)
    mistakes: Mapped[int | None] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    session: Mapped["GameSession"] = relationship(back_populates="result")
