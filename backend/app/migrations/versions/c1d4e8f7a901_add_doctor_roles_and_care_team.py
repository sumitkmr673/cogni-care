"""add doctor roles and care team

Revision ID: c1d4e8f7a901
Revises: 7b3c9d1e2f40
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "c1d4e8f7a901"
down_revision: Union[str, Sequence[str], None] = "7b3c9d1e2f40"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.drop_constraint("ck_users_role", "users", type_="check")
    op.create_check_constraint(
        "ck_users_role", "users", "role IN ('PATIENT', 'CAREGIVER', 'DOCTOR')"
    )
    op.create_table(
        "doctors",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("user_id", sa.UUID(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id", name="uq_doctors_user_id"),
    )
    op.create_table(
        "doctor_patients",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("doctor_id", sa.UUID(), nullable=False),
        sa.Column("patient_id", sa.UUID(), nullable=False),
        sa.Column("assigned_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["doctor_id"], ["doctors.id"]),
        sa.ForeignKeyConstraint(["patient_id"], ["patients.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("doctor_id", "patient_id", name="uq_doctor_patients_pair"),
    )
    op.create_index("ix_doctor_patients_patient_id", "doctor_patients", ["patient_id"])


def downgrade() -> None:
    op.drop_index("ix_doctor_patients_patient_id", table_name="doctor_patients")
    op.drop_table("doctor_patients")
    op.drop_table("doctors")
    op.drop_constraint("ck_users_role", "users", type_="check")
    op.create_check_constraint("ck_users_role", "users", "role IN ('PATIENT', 'CAREGIVER')")
