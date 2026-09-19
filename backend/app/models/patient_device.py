import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, Index, String, func, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.session import Base
from app.identifiers import generate_device_identifier

if TYPE_CHECKING:
    from app.models.patient import Patient


class PatientDevice(Base):
    __tablename__ = "patient_devices"
    __table_args__ = (
        CheckConstraint("status IN ('ACTIVE', 'REVOKED')", name="ck_patient_devices_status"),
        Index(
            "uq_patient_devices_active_patient",
            "patient_id",
            unique=True,
            postgresql_where=text("status = 'ACTIVE'"),
        ),
        Index(
            "uq_patient_devices_active_device",
            "device_identifier",
            unique=True,
            postgresql_where=text("status = 'ACTIVE'"),
        ),
        Index(
            "uq_patient_devices_active_client_device",
            "client_device_id",
            unique=True,
            postgresql_where=text("status = 'ACTIVE' AND client_device_id IS NOT NULL"),
        ),
        Index("ix_patient_devices_patient_id", "patient_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    patient_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("patients.id", ondelete="CASCADE"), nullable=False
    )
    device_identifier: Mapped[str] = mapped_column(
        String(64), nullable=False, default=generate_device_identifier
    )
    client_device_id: Mapped[str | None] = mapped_column(String(128))
    device_name: Mapped[str | None] = mapped_column(String(100))
    device_key_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    status: Mapped[str] = mapped_column(String(20), nullable=False, default="ACTIVE", server_default="ACTIVE")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    last_seen_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

    patient: Mapped["Patient"] = relationship(back_populates="devices")
