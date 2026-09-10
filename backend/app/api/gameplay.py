from datetime import datetime, timezone
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.dependencies import get_current_user, get_db
from app.models.game import Game
from app.models.game_result import GameResult
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.user import User
from app.schemas.gameplay import (
    GameResultResponse,
    GamesResponse,
    GameSessionResponse,
    GameSummary,
    StartGameSessionRequest,
    SubmitGameResultRequest,
)

router = APIRouter(tags=["patient gameplay"])

PATIENT_ACCESS_DESCRIPTION = (
    "Requires a Bearer access token. Patient identity is taken from the JWT, "
    "not from a client-supplied patient ID."
)


def get_current_patient(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Patient:
    if current_user.role != "PATIENT":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Patient access required",
        )
    patient = db.scalar(select(Patient).where(Patient.user_id == current_user.id))
    if patient is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Patient access required",
        )
    return patient


def _active_game(db: Session, game_id: UUID) -> Game:
    game = db.scalar(select(Game).where(Game.id == game_id, Game.is_active.is_(True)))
    if game is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Game not found",
        )
    return game


def _owned_session(db: Session, session_id: UUID, patient_id: UUID) -> GameSession:
    session = db.scalar(select(GameSession).where(GameSession.id == session_id))
    if session is None or session.patient_id != patient_id:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Game session not found",
        )
    return session


@router.get(
    "/games",
    response_model=GamesResponse,
    summary="List available games",
    description="Requires a Bearer access token. Returns active games from the catalog.",
)
def list_games(
    _user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> GamesResponse:
    games = db.scalars(
        select(Game).where(Game.is_active.is_(True)).order_by(Game.name, Game.id)
    ).all()
    return GamesResponse(games=[GameSummary.model_validate(game, from_attributes=True) for game in games])


@router.post(
    "/games/{game_id}/sessions",
    response_model=GameSessionResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Start a game session",
    description=PATIENT_ACCESS_DESCRIPTION,
)
def start_game_session(
    game_id: UUID,
    payload: StartGameSessionRequest,
    patient: Patient = Depends(get_current_patient),
    db: Session = Depends(get_db),
) -> GameSession:
    game = _active_game(db, game_id)
    session = GameSession(
        patient_id=patient.id,
        game_id=game.id,
        difficulty_level=payload.difficulty_level,
        started_at=datetime.now(timezone.utc),
        status="STARTED",
    )
    db.add(session)
    db.commit()
    db.refresh(session)
    return session


@router.post(
    "/games/sessions/{session_id}/result",
    response_model=GameResultResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Submit a game session result",
    description=PATIENT_ACCESS_DESCRIPTION,
)
def submit_game_result(
    session_id: UUID,
    payload: SubmitGameResultRequest,
    patient: Patient = Depends(get_current_patient),
    db: Session = Depends(get_db),
) -> GameResultResponse:
    session = _owned_session(db, session_id, patient.id)
    existing = db.scalar(select(GameResult).where(GameResult.session_id == session.id))
    if existing is not None or session.status != "STARTED":
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Game session result already submitted",
        )

    result = GameResult(
        session_id=session.id,
        score=payload.score,
        accuracy=payload.accuracy,
        correct_answers=payload.correct_answers,
        total_questions=payload.total_questions,
        response_time_ms=payload.response_time_ms,
        mistakes=payload.mistakes,
    )
    session.status = "COMPLETED"
    session.completed_at = datetime.now(timezone.utc)
    db.add(result)
    db.commit()
    db.refresh(result)
    db.refresh(session)
    return GameResultResponse(
        id=result.id,
        session_id=result.session_id,
        score=result.score,
        accuracy=result.accuracy,
        correct_answers=result.correct_answers,
        total_questions=result.total_questions,
        response_time_ms=result.response_time_ms,
        mistakes=result.mistakes,
        session_status=session.status,
        completed_at=session.completed_at,
    )
