from uuid import UUID

from pydantic import BaseModel, Field


class LoginRequest(BaseModel):
    email: str = Field(min_length=1, max_length=255)
    password: str = Field(min_length=1)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class AuthenticatedUserResponse(BaseModel):
    id: UUID
    email: str | None
    display_name: str
    role: str
    is_active: bool
    caregiver_id: UUID | None = None
    patient_id: UUID | None = None
    public_id: str | None = None
    caregiver_type: str | None = None
    phone: str | None = None


class CaregiverRegisterRequest(BaseModel):
    email: str = Field(min_length=3, max_length=255)
    password: str = Field(min_length=8)
    display_name: str = Field(min_length=1, max_length=150)
    caregiver_type: str = Field(default="FAMILY", max_length=50)
    phone: str | None = Field(default=None, max_length=30)


class CaregiverRegisterResponse(BaseModel):
    id: UUID
    caregiver_id: UUID
    public_id: str
    email: str
    display_name: str
    role: str = "CAREGIVER"
    caregiver_type: str
    phone: str | None = None
