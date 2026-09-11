from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field, model_validator

ReminderType = Literal["MEDICATION", "APPOINTMENT", "GAME", "ACTIVITY", "OTHER"]


class ReminderCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=150)
    description: str | None = None
    reminder_type: ReminderType
    scheduled_at: datetime
    is_recurring: bool = False
    recurrence_rule: str | None = Field(default=None, max_length=255)
    is_active: bool = True

    @model_validator(mode="after")
    def validate_recurrence(self) -> "ReminderCreateRequest":
        if self.is_recurring and not self.recurrence_rule:
            raise ValueError("recurrence_rule is required for recurring reminders")
        if not self.is_recurring and self.recurrence_rule is not None:
            raise ValueError("recurrence_rule must be null for non-recurring reminders")
        return self
