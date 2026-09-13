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
    # The base migration already contains these corrected definitions. Keep this
    # revision as a no-op so fresh databases and existing databases converge.
    pass


def downgrade() -> None:
    """Downgrade schema."""
    pass
