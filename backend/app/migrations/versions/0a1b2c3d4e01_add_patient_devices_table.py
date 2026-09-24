"""add patient_devices table

Revision ID: 0a1b2c3d4e01
Revises: f4a1b2c3d001
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID


revision: str = "0a1b2c3d4e01"
down_revision: Union[str, Sequence[str], None] = "a1b2c3d4e002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create patient_devices table
    op.create_table(
        "patient_devices",
        sa.Column("id", UUID(as_uuid=True), primary_key=True),
        sa.Column("patient_id", UUID(as_uuid=True), nullable=False),
        sa.Column("device_identifier", sa.String(64), nullable=False),
        sa.Column("client_device_id", sa.String(128), nullable=True),
        sa.Column("device_name", sa.String(100), nullable=True),
        sa.Column("device_key_hash", sa.String(255), nullable=False),
        sa.Column("status", sa.String(20), nullable=False, server_default="ACTIVE"),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.Column("last_seen_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.CheckConstraint("status IN ('ACTIVE', 'REVOKED')", name="ck_patient_devices_status"),
        sa.ForeignKeyConstraint(
            ["patient_id"],
            ["patients.id"],
            name="fk_patient_devices_patient_id_patients",
            ondelete="CASCADE",
        ),
    )

    # 2. Indexes
    op.create_index(
        "ix_patient_devices_patient_id",
        "patient_devices",
        ["patient_id"],
    )

    # Partial unique index: A patient can have at most ONE ACTIVE device
    op.create_index(
        "uq_patient_devices_active_patient",
        "patient_devices",
        ["patient_id"],
        unique=True,
        postgresql_where=sa.text("status = 'ACTIVE'"),
    )

    # Partial unique index: A device identifier can have at most ONE ACTIVE patient
    op.create_index(
        "uq_patient_devices_active_device",
        "patient_devices",
        ["device_identifier"],
        unique=True,
        postgresql_where=sa.text("status = 'ACTIVE'"),
    )

    # Partial unique index: An app client device can have at most ONE ACTIVE patient
    op.create_index(
        "uq_patient_devices_active_client_device",
        "patient_devices",
        ["client_device_id"],
        unique=True,
        postgresql_where=sa.text("status = 'ACTIVE' AND client_device_id IS NOT NULL"),
    )


def downgrade() -> None:
    op.drop_index("uq_patient_devices_active_client_device", table_name="patient_devices")
    op.drop_index("uq_patient_devices_active_device", table_name="patient_devices")
    op.drop_index("uq_patient_devices_active_patient", table_name="patient_devices")
    op.drop_index("ix_patient_devices_patient_id", table_name="patient_devices")
    op.drop_table("patient_devices")
