"""add created_by_caregiver to reminders

Revision ID: f4a1b2c3d001
Revises: e3f6a9b2c001
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import UUID


revision: str = "f4a1b2c3d001"
down_revision: Union[str, Sequence[str], None] = "e3f6a9b2c001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Add nullable created_by_caregiver_id column
    op.add_column("reminders", sa.Column("created_by_caregiver_id", UUID(as_uuid=True), nullable=True))

    # 2. Backfill existing rows deterministically:
    # Assign the caregiver assigned to patient_id (prioritizing primary, then earliest assigned).
    conn = op.get_bind()
    conn.execute(sa.text("""
        UPDATE reminders r
        SET created_by_caregiver_id = (
            SELECT pc.caregiver_id
            FROM patient_caregivers pc
            WHERE pc.patient_id = r.patient_id
            ORDER BY pc.is_primary DESC, pc.assigned_at ASC
            LIMIT 1
        )
        WHERE r.created_by_caregiver_id IS NULL;
    """))

    # If any reminder belongs to a patient without any assigned caregiver in development data,
    # delete them safely so no fictional creator is fabricated.
    conn.execute(sa.text("""
        DELETE FROM reminders
        WHERE created_by_caregiver_id IS NULL;
    """))

    # 3. Enforce NOT NULL constraint
    op.alter_column("reminders", "created_by_caregiver_id", nullable=False)

    # 4. Create foreign key constraint with CASCADE delete
    op.create_foreign_key(
        "fk_reminders_created_by_caregiver_id_caregivers",
        "reminders",
        "caregivers",
        ["created_by_caregiver_id"],
        ["id"],
        ondelete="CASCADE",
    )

    # 5. Create index
    op.create_index(
        "ix_reminders_created_by_caregiver_id",
        "reminders",
        ["created_by_caregiver_id"],
    )


def downgrade() -> None:
    op.drop_index("ix_reminders_created_by_caregiver_id", table_name="reminders")
    op.drop_constraint("fk_reminders_created_by_caregiver_id_caregivers", "reminders", type_="foreignkey")
    op.drop_column("reminders", "created_by_caregiver_id")
