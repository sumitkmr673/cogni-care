"""add public ids to caregivers and patients

Revision ID: e3f6a9b2c001
Revises: d2e5f8a1b902
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = "e3f6a9b2c001"
down_revision: Union[str, Sequence[str], None] = "d2e5f8a1b902"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add nullable public_id columns
    op.add_column("caregivers", sa.Column("public_id", sa.String(length=20), nullable=True))
    op.add_column("patients", sa.Column("public_id", sa.String(length=20), nullable=True))

    # 2. Backfill existing rows deterministically with uppercase alphanumeric string
    conn = op.get_bind()
    conn.execute(sa.text("""
        UPDATE caregivers
        SET public_id = 'CG-' || UPPER(SUBSTRING(MD5(id::text || 'cg_salt') FROM 1 FOR 8))
        WHERE public_id IS NULL;
    """))
    conn.execute(sa.text("""
        UPDATE patients
        SET public_id = 'PT-' || UPPER(SUBSTRING(MD5(id::text || 'pt_salt') FROM 1 FOR 8))
        WHERE public_id IS NULL;
    """))

    # 3. Enforce NOT NULL constraints
    op.alter_column("caregivers", "public_id", nullable=False)
    op.alter_column("patients", "public_id", nullable=False)

    # 4. Create unique indexes
    op.create_index("ix_caregivers_public_id", "caregivers", ["public_id"], unique=True)
    op.create_index("ix_patients_public_id", "patients", ["public_id"], unique=True)


def downgrade() -> None:
    op.drop_index("ix_patients_public_id", table_name="patients")
    op.drop_index("ix_caregivers_public_id", table_name="caregivers")
    op.drop_column("patients", "public_id")
    op.drop_column("caregivers", "public_id")
