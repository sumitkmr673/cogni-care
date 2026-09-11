from collections.abc import Generator
from uuid import UUID

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import InvalidTokenError
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.user import User
from app.models.caregiver import Caregiver
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.security import decode_access_token

bearer_scheme = HTTPBearer(auto_error=False)


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    unauthorized = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or expired access token",
        headers={"WWW-Authenticate": "Bearer"},
    )
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise unauthorized

    try:
        payload = decode_access_token(credentials.credentials)
        subject = payload.get("sub")
        user_id = UUID(subject) if isinstance(subject, str) else None
    except (InvalidTokenError, ValueError, TypeError):
        raise unauthorized from None

    if user_id is None:
        raise unauthorized

    user = db.scalar(select(User).where(User.id == user_id))
    if user is None or not user.is_active:
        raise unauthorized
    return user


def get_current_caregiver(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Caregiver:
    if current_user.role != "CAREGIVER":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Caregiver access required",
        )

    caregiver = db.scalar(
        select(Caregiver).where(Caregiver.user_id == current_user.id)
    )
    if caregiver is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Caregiver profile not found",
        )
    return caregiver


def get_accessible_patient(
    patient_id: UUID,
    caregiver: Caregiver,
    db: Session,
) -> Patient:
    patient = db.scalar(
        select(Patient)
        .join(PatientCaregiver, PatientCaregiver.patient_id == Patient.id)
        .where(
            Patient.id == patient_id,
            PatientCaregiver.caregiver_id == caregiver.id,
        )
    )
    if patient is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found",
        )
    return patient
