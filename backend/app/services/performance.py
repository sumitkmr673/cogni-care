"""
Performance aggregation service for Cogni-Care.

Computes explainable, non-clinical daily performance metrics strictly derived
from raw gameplay telemetry (accuracy and response time).
"""

from __future__ import annotations

import logging
from datetime import datetime, time, timezone
from decimal import Decimal
from typing import Any
from uuid import UUID
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.game import Game
from app.models.game_result import GameResult
from app.models.game_session import GameSession
from app.models.patient import Patient
from app.models.performance_metric import PerformanceMetric

logger = logging.getLogger(__name__)

DEFAULT_TIMEZONE = "Asia/Kolkata"


def get_patient_tz(tz_name: str | None) -> ZoneInfo:
    """Resolve patient timezone using zoneinfo.ZoneInfo.

    If the timezone is missing, empty, or invalid, fall back to Asia/Kolkata.
    """
    if tz_name:
        try:
            return ZoneInfo(tz_name.strip())
        except (ZoneInfoNotFoundError, ValueError, KeyError):
            logger.warning(
                "Invalid timezone '%s' for patient; falling back to %s",
                tz_name,
                DEFAULT_TIMEZONE,
            )
    return ZoneInfo(DEFAULT_TIMEZONE)


def get_local_day_utc_boundaries(
    dt: datetime, tz: ZoneInfo
) -> tuple[datetime.date, datetime, datetime]:
    """Calculate the patient's local calendar date and corresponding UTC boundaries.

    Ensures the reference datetime is timezone-aware (assumes UTC if naive).
    Returns (local_date, day_start_utc, day_end_utc).
    """
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)

    local_dt = dt.astimezone(tz)
    local_date = local_dt.date()

    day_start_local = datetime.combine(local_date, time.min, tzinfo=tz)
    day_end_local = datetime.combine(local_date, time.max, tzinfo=tz)

    day_start_utc = day_start_local.astimezone(timezone.utc)
    day_end_utc = day_end_local.astimezone(timezone.utc)

    return local_date, day_start_utc, day_end_utc


def calculate_and_upsert_daily_performance(
    db: Session,
    patient_id: UUID,
    patient_tz_name: str | None,
    reference_time: datetime,
) -> PerformanceMetric:
    """Aggregate gameplay telemetry for patient on the local calendar day of reference_time,

    and atomically create or update the corresponding PerformanceMetric record.
    Does NOT commit the database session to maintain caller-managed transactional boundaries.
    """
    tz = get_patient_tz(patient_tz_name)
    metric_date, day_start_utc, day_end_utc = get_local_day_utc_boundaries(reference_time, tz)

    # 1. Query all completed sessions and associated game results for this local day
    completed_stmt = (
        select(
            GameSession.id.label("id"),
            Game.category.label("category"),
            GameResult.accuracy.label("accuracy"),
            GameResult.response_time_ms.label("response_time_ms"),
        )
        .join(Game, Game.id == GameSession.game_id)
        .outerjoin(GameResult, GameResult.session_id == GameSession.id)
        .where(
            GameSession.patient_id == patient_id,
            GameSession.status == "COMPLETED",
            GameSession.completed_at >= day_start_utc,
            GameSession.completed_at <= day_end_utc,
        )
    )
    completed_rows = db.execute(completed_stmt).all()

    # 2. Extract telemetry lists while safely handling row shapes (SQLAlchemy Row / tuple / namedtuple)
    mem_accuracies: list[Decimal] = []
    att_accuracies: list[Decimal] = []
    all_accuracies: list[Decimal] = []
    response_times: list[int] = []

    for row in completed_rows:
        category = getattr(row, "category", row[1] if isinstance(row, (tuple, list)) else None)
        accuracy = getattr(row, "accuracy", row[2] if isinstance(row, (tuple, list)) else None)
        response_time = getattr(row, "response_time_ms", row[3] if isinstance(row, (tuple, list)) else None)

        if accuracy is not None:
            dec_acc = Decimal(str(accuracy)) if not isinstance(accuracy, Decimal) else accuracy
            all_accuracies.append(dec_acc)
            if category == "MEMORY":
                mem_accuracies.append(dec_acc)
            elif category == "CONCENTRATION_ATTENTION":
                att_accuracies.append(dec_acc)

        if response_time is not None:
            response_times.append(int(response_time))

    # 3. Calculate category scores and overall averages (NULL if no non-null values; never 0 for missing)
    memory_score = (
        round(sum(mem_accuracies) / Decimal(len(mem_accuracies)), 2)
        if mem_accuracies
        else None
    )
    attention_score = (
        round(sum(att_accuracies) / Decimal(len(att_accuracies)), 2)
        if att_accuracies
        else None
    )
    average_accuracy = (
        round(sum(all_accuracies) / Decimal(len(all_accuracies)), 2)
        if all_accuracies
        else None
    )
    average_response_time_ms = (
        int(round(sum(response_times) / len(response_times)))
        if response_times
        else None
    )

    # 4. Count completed and started sessions
    games_completed = len(completed_rows)

    started_count = db.scalar(
        select(func.count(GameSession.id)).where(
            GameSession.patient_id == patient_id,
            GameSession.started_at >= day_start_utc,
            GameSession.started_at <= day_end_utc,
        )
    ) or 0

    # total_sessions = max(started_count, games_completed) preserves the existing database invariant
    # ck_performance_metrics_games_lte_sessions (games_completed <= total_sessions) when a session starts
    # on one local calendar day and completes on the next.
    total_sessions = max(started_count, games_completed)

    # 5. Lookup existing daily metric row or create a new one
    metric = db.scalar(
        select(PerformanceMetric)
        .where(
            PerformanceMetric.patient_id == patient_id,
            PerformanceMetric.metric_date == metric_date,
        )
        .with_for_update()
    )

    if metric is None:
        metric = PerformanceMetric(
            patient_id=patient_id,
            metric_date=metric_date,
            memory_score=memory_score,
            attention_score=attention_score,
            average_accuracy=average_accuracy,
            average_response_time_ms=average_response_time_ms,
            games_completed=games_completed,
            total_sessions=total_sessions,
        )
        db.add(metric)
    else:
        metric.memory_score = memory_score
        metric.attention_score = attention_score
        metric.average_accuracy = average_accuracy
        metric.average_response_time_ms = average_response_time_ms
        metric.games_completed = games_completed
        metric.total_sessions = total_sessions

    return metric


def record_daily_performance_metric(
    db: Session,
    session: GameSession,
    patient: Patient,
) -> PerformanceMetric:
    """Record daily performance metric for a patient after completing a game session."""
    reference_time = session.completed_at or session.started_at or datetime.now(timezone.utc)
    return calculate_and_upsert_daily_performance(
        db=db,
        patient_id=patient.id,
        patient_tz_name=patient.timezone,
        reference_time=reference_time,
    )
