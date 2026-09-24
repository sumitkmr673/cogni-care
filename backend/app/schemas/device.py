from datetime import date, datetime
from uuid import UUID

from pydantic import BaseModel, Field

from app.schemas.auth import TokenResponse


class DeviceProvisionRequest(BaseModel):
    device_name: str | None = Field(default=None, max_length=100)
    client_device_id: str | None = Field(default=None, max_length=128)


class DeviceProvisionResponse(BaseModel):
    device_id: UUID
    device_identifier: str
    device_key: str
    patient_id: UUID
    patient_public_id: str
    display_name: str
    status: str
    created_at: datetime


class DeviceSummaryResponse(BaseModel):
    device_id: UUID
    device_identifier: str
    device_name: str | None
    client_device_id: str | None
    status: str
    created_at: datetime
    last_seen_at: datetime | None
    revoked_at: datetime | None


class DeviceLoginRequest(BaseModel):
    device_identifier: str = Field(min_length=1, max_length=64)
    device_key: str = Field(min_length=1)


class DeviceRevokeResponse(BaseModel):
    device_identifier: str
    status: str
    revoked_at: datetime


class PatientLoginByIdRequest(BaseModel):
    public_id: str = Field(min_length=1, max_length=30)
    client_device_id: str | None = Field(default=None, max_length=128)
    device_name: str | None = Field(default=None, max_length=100)


class PatientRegisterRequest(BaseModel):
    display_name: str = Field(min_length=1, max_length=150)
    date_of_birth: date | None = None
    gender: str | None = Field(default=None, max_length=50)
    preferred_language: str | None = Field(default="English", max_length=50)
    client_device_id: str | None = Field(default=None, max_length=128)
    device_name: str | None = Field(default=None, max_length=100)


class PatientRegisterResponse(BaseModel):
    patient_id: UUID
    patient_public_id: str
    display_name: str
    device_identifier: str
    device_key: str
    token: TokenResponse


class PatientLoginByIdResponse(PatientRegisterResponse):
    pass
