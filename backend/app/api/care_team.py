from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.api.dependencies import get_accessible_patient, get_current_caregiver, get_db
from app.models.caregiver import Caregiver
from app.models.patient_caregiver import PatientCaregiver
from app.models.user import User
from app.schemas.dashboard import CareTeamAssignmentRequest, CareTeamMember, CareTeamResponse

router = APIRouter(tags=["care team"])


def _primary_patient(patient_id: UUID, caregiver: Caregiver, db: Session):
    patient = get_accessible_patient(patient_id, caregiver, db)
    link = db.scalar(
        select(PatientCaregiver).where(
            PatientCaregiver.patient_id == patient_id,
            PatientCaregiver.caregiver_id == caregiver.id,
            PatientCaregiver.is_primary.is_(True),
        )
    )
    if link is None:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Primary caregiver access required")
    return patient


@router.get(
    "/patients/{patient_id}/care-team",
    response_model=CareTeamResponse,
    summary="View a patient's care team",
)
def get_care_team(
    patient_id: UUID,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> CareTeamResponse:
    get_accessible_patient(patient_id, caregiver, db)
    rows = db.execute(
        select(PatientCaregiver, Caregiver, User.display_name)
        .join(Caregiver, Caregiver.id == PatientCaregiver.caregiver_id)
        .join(User, User.id == Caregiver.user_id)
        .where(PatientCaregiver.patient_id == patient_id)
        .order_by(PatientCaregiver.is_primary.desc(), User.display_name)
    ).all()
    return CareTeamResponse(
        patient_id=patient_id,
        members=[
            CareTeamMember(
                caregiver_id=linked_caregiver.id,
                display_name=name,
                caregiver_type=linked_caregiver.caregiver_type,
                is_primary=link.is_primary,
            )
            for link, linked_caregiver, name in rows
        ],
    )


@router.post(
    "/patients/{patient_id}/care-team",
    response_model=CareTeamMember,
    status_code=status.HTTP_201_CREATED,
    summary="Add a caregiver to a patient's care team",
)
def add_care_team_member(
    patient_id: UUID,
    assignment: CareTeamAssignmentRequest,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> CareTeamMember:
    _primary_patient(patient_id, caregiver, db)
    assigned = db.scalar(
        select(Caregiver)
        .join(User, User.id == Caregiver.user_id)
        .where(Caregiver.id == assignment.caregiver_id, User.role == "CAREGIVER")
    )
    if assigned is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Caregiver not found")
    existing = db.scalar(
        select(PatientCaregiver).where(
            PatientCaregiver.patient_id == patient_id,
            PatientCaregiver.caregiver_id == assigned.id,
        )
    )
    if existing is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Caregiver is already assigned")
    if assignment.is_primary:
        existing_primary = db.scalar(
            select(PatientCaregiver).where(
                PatientCaregiver.patient_id == patient_id,
                PatientCaregiver.is_primary.is_(True),
            )
        )
        if existing_primary is not None:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Patient already has a primary caregiver")
    link = PatientCaregiver(
        patient_id=patient_id,
        caregiver_id=assigned.id,
        is_primary=assignment.is_primary,
    )
    db.add(link)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Caregiver assignment already exists") from None
    name = db.scalar(select(User.display_name).where(User.id == assigned.user_id))
    return CareTeamMember(
        caregiver_id=assigned.id,
        display_name=name,
        caregiver_type=assigned.caregiver_type,
        is_primary=link.is_primary,
    )


@router.put(
    "/patients/{patient_id}/care-team/{caregiver_id}/primary",
    response_model=CareTeamMember,
    summary="Transfer primary caregiver responsibility",
)
def transfer_primary_caregiver(
    patient_id: UUID,
    caregiver_id: UUID,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> CareTeamMember:
    _primary_patient(patient_id, caregiver, db)
    links = db.scalars(
        select(PatientCaregiver)
        .where(PatientCaregiver.patient_id == patient_id)
        .with_for_update()
    ).all()
    current = next(link for link in links if link.caregiver_id == caregiver.id and link.is_primary)
    target = next((link for link in links if link.caregiver_id == caregiver_id), None)
    if target is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Caregiver assignment not found")
    if target.is_primary:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Caregiver is already primary")

    current.is_primary = False
    db.flush()
    target.is_primary = True
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Primary caregiver transfer failed") from None

    assigned = db.scalar(select(Caregiver).where(Caregiver.id == target.caregiver_id))
    name = db.scalar(select(User.display_name).where(User.id == assigned.user_id))
    return CareTeamMember(
        caregiver_id=assigned.id,
        display_name=name,
        caregiver_type=assigned.caregiver_type,
        is_primary=True,
    )


@router.delete(
    "/patients/{patient_id}/care-team/{caregiver_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Remove a caregiver from a patient's care team",
)
def remove_care_team_member(
    patient_id: UUID,
    caregiver_id: UUID,
    caregiver: Caregiver = Depends(get_current_caregiver),
    db: Session = Depends(get_db),
) -> None:
    _primary_patient(patient_id, caregiver, db)
    link = db.scalar(
        select(PatientCaregiver).where(
            PatientCaregiver.patient_id == patient_id,
            PatientCaregiver.caregiver_id == caregiver_id,
        )
    )
    if link is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Caregiver assignment not found")
    if link.is_primary:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="The primary caregiver cannot be removed")
    db.delete(link)
    db.commit()
