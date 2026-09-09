"""align core domain schema

Revision ID: 7b3c9d1e2f40
Revises: a06f3f5966ec
Create Date: 2026-09-09

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "7b3c9d1e2f40"
down_revision: Union[str, Sequence[str], None] = "a06f3f5966ec"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.add_column(
        "patient_caregivers",
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
    )

    op.drop_index(
        "ix_performance_metrics_patient_date",
        table_name="performance_metrics",
    )

    op.create_unique_constraint(
        "uq_performance_metrics_patient_date",
        "performance_metrics",
        ["patient_id", "metric_date"],
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_constraint(
        "uq_performance_metrics_patient_date",
        "performance_metrics",
        type_="unique",
    )

    op.create_index(
        "ix_performance_metrics_patient_date",
        "performance_metrics",
        ["patient_id", "metric_date"],
        unique=True,
    )

    op.drop_column("patient_caregivers", "updated_at")
