"""add pattern recall game

Revision ID: a1b2c3d4e002
Revises: f4a1b2c3d001
"""
from typing import Sequence, Union
import uuid

from alembic import op
import sqlalchemy as sa


revision: str = "a1b2c3d4e002"
down_revision: Union[str, Sequence[str], None] = "f4a1b2c3d001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    conn = op.get_bind()
    conn.execute(
        sa.text("""
            INSERT INTO games (id, code, name, category, description, is_active, created_at, updated_at)
            VALUES (
                :id,
                'PATTERN_RECALL',
                'Pattern Recall',
                'MEMORY',
                'Sequential visual pattern memory and recall game.',
                true,
                now(),
                now()
            )
            ON CONFLICT (code) DO NOTHING;
        """),
        {"id": uuid.uuid4()},
    )


def downgrade() -> None:
    conn = op.get_bind()
    conn.execute(sa.text("DELETE FROM games WHERE code = 'PATTERN_RECALL';"))
