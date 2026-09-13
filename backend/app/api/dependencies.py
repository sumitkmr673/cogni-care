from collections.abc import Generator
from dataclasses import dataclass
from uuid import UUID

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import InvalidTokenError
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import SessionLocal
from app.models.user import User
from app.models.caregiver import Caregiver
from app.models.doctor import Doctor
from app.models.doctor_patient import DoctorPatient
from app.models.patient import Patient
from app.models.patient_caregiver import PatientCaregiver
from app.security import decode_access_token

bearer_scheme = HTTPBearer(auto_error=False)


@dataclass(frozen=True)
class PatientAccessor:
    role: str
    caregiver: Caregiver | None = None
    doctor: Doctor | None = None


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


def get_current_doctor(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Doctor:
    if current_user.role != "DOCTOR":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Doctor access required",
        )
    doctor = db.scalar(select(Doctor).where(Doctor.user_id == current_user.id))
    if doctor is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Doctor profile not found",
        )
    return doctor


def get_current_patient_accessor(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> PatientAccessor:
    if current_user.role == "CAREGIVER":
        return PatientAccessor(role=current_user.role, caregiver=get_current_caregiver(current_user, db))
    if current_user.role == "DOCTOR":
        return PatientAccessor(role=current_user.role, doctor=get_current_doctor(current_user, db))
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Caregiver or doctor access required",
    )


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


def get_accessible_patient_for_doctor(
    patient_id: UUID,
    doctor: Doctor,
    db: Session,
) -> Patient:
    patient = db.scalar(
        select(Patient)
        .join(DoctorPatient, DoctorPatient.patient_id == Patient.id)
        .where(Patient.id == patient_id, DoctorPatient.doctor_id == doctor.id)
    )
    if patient is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found",
        )
    return patient


def get_accessible_patient_for_accessor(
    patient_id: UUID,
    accessor: PatientAccessor,
    db: Session,
) -> Patient:
    if accessor.caregiver is not None:
        return get_accessible_patient(patient_id, accessor.caregiver, db)
    if accessor.doctor is not None:
        return get_accessible_patient_for_doctor(patient_id, accessor.doctor, db)
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Caregiver or doctor access required",
    )
