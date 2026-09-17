from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.dependencies import get_current_user, get_db
from app.identifiers import generate_caregiver_public_id
from app.models.caregiver import Caregiver
from app.models.user import User
from app.schemas.auth import (
    AuthenticatedUserResponse,
    CaregiverRegisterRequest,
    CaregiverRegisterResponse,
    LoginRequest,
    TokenResponse,
)
from app.security import create_access_token, hash_password, verify_password

router = APIRouter(prefix="/auth", tags=["authentication"])

INVALID_CREDENTIALS = "Invalid email or password"


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


@router.get("/me", response_model=AuthenticatedUserResponse)
def current_user(
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> AuthenticatedUserResponse:
    caregiver_id = None
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

    return AuthenticatedUserResponse(
        id=user.id,
        email=user.email,
        display_name=user.display_name,
        role=user.role,
        is_active=user.is_active,
        caregiver_id=caregiver_id,
        public_id=public_id,
        caregiver_type=caregiver_type,
        phone=phone,
    )
