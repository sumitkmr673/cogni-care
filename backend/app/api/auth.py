import secrets
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.dependencies import get_current_user, get_db
from app.identifiers import (
    generate_caregiver_public_id,
    generate_device_identifier,
    generate_patient_public_id,
)
from app.models.caregiver import Caregiver
from app.models.patient import Patient
from app.models.patient_device import PatientDevice
from app.models.user import User
from app.schemas.auth import (
    AuthenticatedUserResponse,
    CaregiverRegisterRequest,
    CaregiverRegisterResponse,
    LoginRequest,
    TokenResponse,
)
from app.schemas.device import (
    DeviceLoginRequest,
    PatientRegisterRequest,
    PatientRegisterResponse,
)
from app.security import create_access_token, hash_password, verify_password

router = APIRouter(prefix="/auth", tags=["authentication"])

INVALID_CREDENTIALS = "Invalid email or password"
INVALID_DEVICE_CREDENTIALS = "Invalid device credentials or device has been revoked"


@router.post("/register", response_model=CaregiverRegisterResponse, status_code=status.HTTP_201_CREATED)
def register_caregiver(
    payload: CaregiverRegisterRequest,
    db: Session = Depends(get_db),
) -> CaregiverRegisterResponse:
    email = payload.email.strip().lower()
    existing = db.scalar(select(User).where(User.email == email))
    if existing is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email already exists",
        )

    valid_types = ("FAMILY", "DOCTOR", "PROFESSIONAL_CAREGIVER", "OTHER")
    caregiver_type = payload.caregiver_type.strip().upper() if payload.caregiver_type else "FAMILY"
    if caregiver_type not in valid_types:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"caregiver_type must be one of {valid_types}",
        )

    user = User(
        email=email,
        password_hash=hash_password(payload.password),
        display_name=payload.display_name.strip(),
        role="CAREGIVER",
        is_active=True,
    )
    db.add(user)
    db.flush()

    for _ in range(10):
        code = generate_caregiver_public_id()
        if not db.scalar(select(Caregiver).where(Caregiver.public_id == code)):
            break
    else:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Could not generate unique caregiver ID",
        )

    caregiver = Caregiver(
        user_id=user.id,
        public_id=code,
        caregiver_type=caregiver_type,
        phone=payload.phone.strip() if payload.phone else None,
    )
    db.add(caregiver)
    db.commit()
    db.refresh(caregiver)

    return CaregiverRegisterResponse(
        id=user.id,
        caregiver_id=caregiver.id,
        public_id=caregiver.public_id,
        email=user.email,
        display_name=user.display_name,
        role=user.role,
        caregiver_type=caregiver.caregiver_type,
        phone=caregiver.phone,
    )


@router.post("/login", response_model=TokenResponse)
def login(credentials: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    email = credentials.email.strip().lower()
    user = db.scalar(select(User).where(User.email == email))
    if (
        user is None
        or not user.is_active
        or not verify_password(credentials.password, user.password_hash)
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=INVALID_CREDENTIALS,
            headers={"WWW-Authenticate": "Bearer"},
        )

    return TokenResponse(access_token=create_access_token(user.id))


@router.post("/device-login", response_model=TokenResponse)
def login_device(payload: DeviceLoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    try:
        device = db.scalar(
            select(PatientDevice).where(PatientDevice.device_identifier == payload.device_identifier.strip())
        )
        if device is None or device.status != "ACTIVE":
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=INVALID_DEVICE_CREDENTIALS,
                headers={"WWW-Authenticate": "Bearer"},
            )

        if not verify_password(payload.device_key, device.device_key_hash):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail=INVALID_DEVICE_CREDENTIALS,
                headers={"WWW-Authenticate": "Bearer"},
            )

        patient = db.scalar(select(Patient).where(Patient.id == device.patient_id))
        if patient is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Patient not found",
                headers={"WWW-Authenticate": "Bearer"},
            )

        user = db.scalar(select(User).where(User.id == patient.user_id))
        if user is None or not user.is_active:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Patient account is inactive",
                headers={"WWW-Authenticate": "Bearer"},
            )

        device.last_seen_at = datetime.now(timezone.utc)
        db.commit()

        return TokenResponse(access_token=create_access_token(user.id))
    except HTTPException:
        db.rollback()
        raise
    except Exception as exc:
        db.rollback()
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Device login error ({type(exc).__name__}): {str(exc)}",
        )


@router.post("/patient/register", response_model=PatientRegisterResponse, status_code=status.HTTP_201_CREATED)
def register_patient(payload: PatientRegisterRequest, db: Session = Depends(get_db)) -> PatientRegisterResponse:
    try:
        client_device_id = payload.client_device_id.strip() if payload.client_device_id else None

        if client_device_id:
            existing_device = db.scalar(
                select(PatientDevice).where(
                    PatientDevice.client_device_id == client_device_id,
                    PatientDevice.status == "ACTIVE",
                )
            )
            if existing_device is not None:
                raise HTTPException(
                    status_code=status.HTTP_409_CONFLICT,
                    detail="This device is already active for another patient",
                )

        user = User(
            display_name=payload.display_name.strip(),
            role="PATIENT",
            is_active=True,
        )
        db.add(user)
        db.flush()

        for _ in range(10):
            code = generate_patient_public_id()
            if not db.scalar(select(Patient).where(Patient.public_id == code)):
                break
        else:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Could not generate unique patient ID",
            )

        patient = Patient(
            user_id=user.id,
            public_id=code,
            date_of_birth=payload.date_of_birth,
            gender=payload.gender.strip() if payload.gender else None,
            preferred_language=payload.preferred_language.strip() if payload.preferred_language else "English",
        )
        db.add(patient)
        db.flush()

        device_identifier = generate_device_identifier()
        device_key = secrets.token_urlsafe(32)
        device = PatientDevice(
            patient_id=patient.id,
            device_identifier=device_identifier,
            client_device_id=client_device_id,
            device_name=payload.device_name.strip() if payload.device_name else None,
            device_key_hash=hash_password(device_key),
            status="ACTIVE",
        )
        db.add(device)
        db.commit()
        db.refresh(patient)

        access_token = create_access_token(user.id)
        return PatientRegisterResponse(
            patient_id=patient.id,
            patient_public_id=patient.public_id,
            display_name=user.display_name,
            device_identifier=device.device_identifier,
            device_key=device_key,
            token=TokenResponse(access_token=access_token),
        )
    except HTTPException:
        db.rollback()
        raise
    except Exception as exc:
        db.rollback()
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Patient register error ({type(exc).__name__}): {str(exc)}",
        )


@router.get("/me", response_model=AuthenticatedUserResponse)
def current_user(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> AuthenticatedUserResponse:
    caregiver_id = None
    patient_id = None
    public_id = None
    caregiver_type = None
    phone = None

    if user.role == "CAREGIVER":
        caregiver = db.scalar(
            select(Caregiver).where(Caregiver.user_id == user.id)
        )
        if caregiver is not None:
            caregiver_id = caregiver.id
            public_id = caregiver.public_id
            caregiver_type = caregiver.caregiver_type
            phone = caregiver.phone
    elif user.role == "PATIENT":
        patient = db.scalar(
            select(Patient).where(Patient.user_id == user.id)
        )
        if patient is not None:
            patient_id = patient.id
            public_id = patient.public_id

    return AuthenticatedUserResponse(
        id=user.id,
        email=user.email,
        display_name=user.display_name,
        role=user.role,
        is_active=user.is_active,
        caregiver_id=caregiver_id,
        patient_id=patient_id,
        public_id=public_id,
        caregiver_type=caregiver_type,
        phone=phone,
    )
