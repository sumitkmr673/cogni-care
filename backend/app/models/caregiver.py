import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, String, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base
from app.identifiers import generate_caregiver_public_id

if TYPE_CHECKING:
    from app.models.patient import Patient
    from app.models.patient_caregiver import PatientCaregiver
    from app.models.reminder import Reminder
    from app.models.user import User


class Caregiver(Base):
    __tablename__ = "caregivers"
    __table_args__ = (
        UniqueConstraint("user_id", name="uq_caregivers_user_id"),
        CheckConstraint(
            "caregiver_type IN ('FAMILY', 'DOCTOR', 'PROFESSIONAL_CAREGIVER', 'OTHER')",
            name="ck_caregivers_type",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=False
    )
    public_id: Mapped[str] = mapped_column(
        String(20), unique=True, index=True, nullable=False, default=generate_caregiver_public_id
    )
    caregiver_type: Mapped[str] = mapped_column(String(50), nullable=False)
    phone: Mapped[str | None] = mapped_column(String(30))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )

    def __init__(self, **kwargs):
        if "public_id" not in kwargs:
            kwargs["public_id"] = generate_caregiver_public_id()
        super().__init__(**kwargs)

    user: Mapped["User"] = relationship(back_populates="caregiver")
    patient_links: Mapped[list["PatientCaregiver"]] = relationship(
        back_populates="caregiver", cascade="save-update, merge"
    )
    patients: Mapped[list["Patient"]] = relationship(
        secondary="patient_caregivers",
        back_populates="caregivers",
        viewonly=True,
    )
    created_reminders: Mapped[list["Reminder"]] = relationship(
        back_populates="created_by_caregiver", cascade="all, delete-orphan"
    )
