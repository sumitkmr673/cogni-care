import uuid
from datetime import date, datetime
from typing import TYPE_CHECKING

from sqlalchemy import Date, DateTime, ForeignKey, String, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base

if TYPE_CHECKING:
    from app.models.caregiver import Caregiver
    from app.models.game_session import GameSession
    from app.models.performance_metric import PerformanceMetric
    from app.models.reminder import Reminder
    from app.models.user import User


class Patient(Base):
    __tablename__ = "patients"
    __table_args__ = (UniqueConstraint("user_id", name="uq_patients_user_id"),)

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False
    )
    date_of_birth: Mapped[date | None] = mapped_column(Date)
    gender: Mapped[str | None] = mapped_column(String(50))
    preferred_language: Mapped[str | None] = mapped_column(String(50))
    timezone: Mapped[str | None] = mapped_column(String(100))
    profile_photo_ref: Mapped[str | None] = mapped_column(String(500))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )

    user: Mapped["User"] = relationship(back_populates="patient")
    caregiver_links: Mapped[list["PatientCaregiver"]] = relationship(
        back_populates="patient", cascade="save-update, merge"
    )
    caregivers: Mapped[list["Caregiver"]] = relationship(
        secondary="patient_caregivers",
        back_populates="patients",
        viewonly=True,
    )
    game_sessions: Mapped[list["GameSession"]] = relationship(
        back_populates="patient", cascade="save-update, merge"
    )
    performance_metrics: Mapped[list["PerformanceMetric"]] = relationship(
        back_populates="patient", cascade="save-update, merge"
    )
    reminders: Mapped[list["Reminder"]] = relationship(
        back_populates="patient", cascade="save-update, merge"
    )
