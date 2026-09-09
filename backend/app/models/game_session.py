import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, Index, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base

if TYPE_CHECKING:
    from app.models.game import Game
    from app.models.game_result import GameResult
    from app.models.patient import Patient


class GameSession(Base):
    __tablename__ = "game_sessions"
    __table_args__ = (
        CheckConstraint(
            "difficulty_level IN (1, 2, 3)",
            name="ck_game_sessions_difficulty_level",
        ),
        CheckConstraint(
            "status IN ('STARTED', 'COMPLETED', 'ABANDONED')",
            name="ck_game_sessions_status",
        ),
        Index("ix_game_sessions_patient_started", "patient_id", "started_at"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    patient_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("patients.id"), nullable=False
    )
    game_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("games.id"), nullable=False
    )
    difficulty_level: Mapped[int] = mapped_column(nullable=False)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    status: Mapped[str] = mapped_column(String(20), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    patient: Mapped["Patient"] = relationship(back_populates="game_sessions")
    game: Mapped["Game"] = relationship(back_populates="game_sessions")
    result: Mapped["GameResult | None"] = relationship(
        back_populates="session", uselist=False, cascade="save-update, merge"
    )
