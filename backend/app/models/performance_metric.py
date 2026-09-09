import uuid
from datetime import date, datetime
from decimal import Decimal
from typing import TYPE_CHECKING

from sqlalchemy import (
    CheckConstraint,
    Date,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    UniqueConstraint,
    func,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base

if TYPE_CHECKING:
    from app.models.patient import Patient


class PerformanceMetric(Base):
    __tablename__ = "performance_metrics"
    __table_args__ = (
        UniqueConstraint(
            "patient_id",
            "metric_date",
            name="uq_performance_metrics_patient_date",
        ),
        CheckConstraint(
            "memory_score IS NULL OR (memory_score >= 0 AND memory_score <= 100)",
            name="ck_performance_metrics_memory_score",
        ),
        CheckConstraint(
            "attention_score IS NULL OR (attention_score >= 0 AND attention_score <= 100)",
            name="ck_performance_metrics_attention_score",
        ),
        CheckConstraint(
            "average_accuracy IS NULL OR (average_accuracy >= 0 AND average_accuracy <= 100)",
            name="ck_performance_metrics_accuracy",
        ),
        CheckConstraint(
            "average_response_time_ms IS NULL OR average_response_time_ms >= 0",
            name="ck_performance_metrics_response_nonnegative",
        ),
        CheckConstraint("games_completed >= 0", name="ck_performance_metrics_games_nonnegative"),
        CheckConstraint("total_sessions >= 0", name="ck_performance_metrics_sessions_nonnegative"),
        CheckConstraint(
            "games_completed <= total_sessions",
            name="ck_performance_metrics_games_lte_sessions",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    patient_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("patients.id"), nullable=False
    )
    metric_date: Mapped[date] = mapped_column(Date, nullable=False)
    memory_score: Mapped[Decimal | None] = mapped_column(Numeric(5, 2))
    attention_score: Mapped[Decimal | None] = mapped_column(Numeric(5, 2))
    average_accuracy: Mapped[Decimal | None] = mapped_column(Numeric(5, 2))
    average_response_time_ms: Mapped[int | None] = mapped_column(Integer)
    games_completed: Mapped[int] = mapped_column(Integer, nullable=False, default=0, server_default="0")
    total_sessions: Mapped[int] = mapped_column(Integer, nullable=False, default=0, server_default="0")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )

    patient: Mapped["Patient"] = relationship(back_populates="performance_metrics")
