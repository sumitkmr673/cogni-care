"""unify caregiver roles and drop doctor tables

Revision ID: d2e5f8a1b902
Revises: c1d4e8f7a901
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "d2e5f8a1b902"
down_revision: Union[str, Sequence[str], None] = "c1d4e8f7a901"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    conn = op.get_bind()

    # 1. Migrate doctors into caregivers table
    conn.execute(sa.text("""
        INSERT INTO caregivers (id, user_id, caregiver_type, phone, created_at, updated_at)
        SELECT d.id, d.user_id, 'DOCTOR', NULL, d.created_at, d.updated_at
        FROM doctors d
        WHERE NOT EXISTS (
            SELECT 1 FROM caregivers c WHERE c.user_id = d.user_id
        )
    """))

    # 2. Migrate doctor_patients assignments into patient_caregivers (as secondary caregivers)
    conn.execute(sa.text("""
        INSERT INTO patient_caregivers (id, patient_id, caregiver_id, is_primary, assigned_at, created_at, updated_at)
        SELECT dp.id, dp.patient_id, c.id, FALSE, dp.assigned_at, dp.created_at, dp.updated_at
        FROM doctor_patients dp
        JOIN doctors d ON dp.doctor_id = d.id
        JOIN caregivers c ON c.user_id = d.user_id
        WHERE NOT EXISTS (
            SELECT 1 FROM patient_caregivers pc
            WHERE pc.patient_id = dp.patient_id AND pc.caregiver_id = c.id
        )
    """))

    # 3. Update users with role='DOCTOR' to role='CAREGIVER'
    conn.execute(sa.text("""
        UPDATE users SET role = 'CAREGIVER' WHERE role = 'DOCTOR'
    """))

    # 4. Drop doctor_patients table
    op.drop_index("ix_doctor_patients_patient_id", table_name="doctor_patients")
    op.drop_table("doctor_patients")

    # 5. Drop doctors table
    op.drop_table("doctors")

    # 6. Update ck_users_role constraint on users table
    op.drop_constraint("ck_users_role", "users", type_="check")
    op.create_check_constraint(
        "ck_users_role", "users", "role IN ('PATIENT', 'CAREGIVER')"
    )


def downgrade() -> None:
    # 1. Update ck_users_role to include DOCTOR
    op.drop_constraint("ck_users_role", "users", type_="check")
    op.create_check_constraint(
        "ck_users_role", "users", "role IN ('PATIENT', 'CAREGIVER', 'DOCTOR')"
    )

    # 2. Recreate doctors table
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

    # 3. Recreate doctor_patients table
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

    # 4. Restore doctor records for caregivers with caregiver_type = 'DOCTOR'
    conn = op.get_bind()
    conn.execute(sa.text("""
        INSERT INTO doctors (id, user_id, created_at, updated_at)
        SELECT c.id, c.user_id, c.created_at, c.updated_at
        FROM caregivers c
        WHERE c.caregiver_type = 'DOCTOR'
        ON CONFLICT (user_id) DO NOTHING
    """))

    conn.execute(sa.text("""
        UPDATE users SET role = 'DOCTOR'
        WHERE id IN (SELECT user_id FROM caregivers WHERE caregiver_type = 'DOCTOR')
    """))

    conn.execute(sa.text("""
        INSERT INTO doctor_patients (id, doctor_id, patient_id, assigned_at, created_at, updated_at)
        SELECT pc.id, d.id, pc.patient_id, pc.assigned_at, pc.created_at, pc.updated_at
        FROM patient_caregivers pc
        JOIN caregivers c ON pc.caregiver_id = c.id
        JOIN doctors d ON d.user_id = c.user_id
        WHERE c.caregiver_type = 'DOCTOR'
        ON CONFLICT (doctor_id, patient_id) DO NOTHING
    """))
