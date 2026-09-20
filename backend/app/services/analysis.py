"""
Rule-Based Performance Analysis and Inference Engine for Cogni-Care.

Deterministic, non-clinical analysis service that derives explainable performance
observations by comparing recent gameplay metrics against an established baseline window.

NOTE ON THRESHOLDS:
All thresholds used below (such as +/-5.0 points and -400/+500ms) are configurable
heuristic implementation thresholds designed for this rule-based MVP.
They are NOT clinical cutoffs, medical criteria, or diagnostic standards.
The system does not diagnose dementia, cognitive impairment, or clinical risk,
and does not generate medical recommendations.
"""

from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from typing import Sequence
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.performance_metric import PerformanceMetric
from app.schemas.analysis import (
    AnalysisSummary,
    DataWindowInfo,
    EvidenceData,
    PatientAnalysisResponse,
    PerformanceObservation,
)

# ---------------------------------------------------------------------------
# Configurable MVP Heuristic Thresholds (Deterministic, Non-Clinical)
# ---------------------------------------------------------------------------
MIN_ACTIVE_DAYS_FOR_ANALYSIS = 3
MAX_RECENT_DAYS = 3
MAX_BASELINE_DAYS = 10

# Percentage points threshold on a 0-100 scale
ACCURACY_CHANGE_THRESHOLD = 5.0
MEMORY_SCORE_CHANGE_THRESHOLD = 5.0
ATTENTION_SCORE_CHANGE_THRESHOLD = 5.0

# Milliseconds change threshold for response time
RESPONSE_TIME_FASTER_THRESHOLD_MS = -400  # ms delta <= -400ms is faster pace
RESPONSE_TIME_SLOWER_THRESHOLD_MS = 500   # ms delta >= +500ms is slower pace


def _safe_float_avg(values: Sequence[float | Decimal | int | None]) -> float | None:
    """Calculate the arithmetic mean of non-null values, rounded to 2 decimal places."""
    valid = [float(v) for v in values if v is not None]
    if not valid:
        return None
    return round(sum(valid) / len(valid), 2)


def analyze_patient_performance(
    db: Session,
    patient_id: UUID,
    reference_time: datetime | None = None,
) -> PatientAnalysisResponse:
    """Perform deterministic rule-based analysis on patient gameplay metrics.

    Compares the recent active gameplay window against the prior baseline window
    to generate structured, non-clinical performance observations.
    """
    now = reference_time or datetime.now(timezone.utc)

    # 1. Fetch all chronological daily metrics for this patient
    metrics = db.scalars(
        select(PerformanceMetric)
        .where(PerformanceMetric.patient_id == patient_id)
        .order_by(PerformanceMetric.metric_date.asc())
    ).all()

    total_active_days = len(metrics)

    # 2. Check Data Sufficiency
    if total_active_days < MIN_ACTIVE_DAYS_FOR_ANALYSIS:
        start_date = metrics[0].metric_date if metrics else None
        end_date = metrics[-1].metric_date if metrics else None
        games_count = sum(m.games_completed for m in metrics)

        window_info = DataWindowInfo(
            total_active_days=total_active_days,
            recent_days_count=total_active_days,
            baseline_days_count=0,
            recent_start_date=start_date,
            recent_end_date=end_date,
            baseline_start_date=None,
            baseline_end_date=None,
            recent_games_completed=games_count,
        )

        return PatientAnalysisResponse(
            patient_id=patient_id,
            generated_at=now,
            data_sufficiency="INSUFFICIENT",
            data_window=window_info,
            summary=AnalysisSummary(
                headline="Performance data is currently limited; at least 3 active days of gameplay are needed to identify reliable trends.",
                primary_direction="INSUFFICIENT_DATA",
            ),
            observations=[
                PerformanceObservation(
                    category="DATA_SUFFICIENCY",
                    direction="INSUFFICIENT_DATA",
                    severity="INFO",
                    title="Data Sufficiency",
                    message=(
                        f"At least {MIN_ACTIVE_DAYS_FOR_ANALYSIS} active days of gameplay are required "
                        f"to establish a comparative baseline. Currently, {total_active_days} active day(s) "
                        f"with {games_count} completed session(s) are recorded."
                    ),
                    evidence=EvidenceData(
                        recent_average=float(total_active_days),
                        delta=None,
                        unit="active days",
                    ),
                )
            ],
        )

    # 3. Partition into Recent and Baseline Windows
    # Recent window: up to MAX_RECENT_DAYS, preserving at least 2 baseline days
    recent_count = min(MAX_RECENT_DAYS, max(1, total_active_days - 2))
    recent_metrics = metrics[-recent_count:]
    baseline_candidates = metrics[:-recent_count]
    baseline_metrics = baseline_candidates[-MAX_BASELINE_DAYS:]

    recent_games = sum(m.games_completed for m in recent_metrics)

    window_info = DataWindowInfo(
        total_active_days=total_active_days,
        recent_days_count=len(recent_metrics),
        baseline_days_count=len(baseline_metrics),
        recent_start_date=recent_metrics[0].metric_date,
        recent_end_date=recent_metrics[-1].metric_date,
        baseline_start_date=baseline_metrics[0].metric_date,
        baseline_end_date=baseline_metrics[-1].metric_date,
        recent_games_completed=recent_games,
    )

    observations: list[PerformanceObservation] = []
    directions_for_summary: list[str] = []

    # 4. Evaluate Overall Accuracy
    rec_acc = _safe_float_avg([m.average_accuracy for m in recent_metrics])
    base_acc = _safe_float_avg([m.average_accuracy for m in baseline_metrics])

    if rec_acc is not None and base_acc is not None:
        delta_acc = round(rec_acc - base_acc, 1)
        if delta_acc >= ACCURACY_CHANGE_THRESHOLD:
            direction = "IMPROVING"
            severity = "POSITIVE"
            msg = "Overall gameplay accuracy has shown positive improvement compared with the previous baseline."
        elif delta_acc <= -ACCURACY_CHANGE_THRESHOLD:
            direction = "LOWER"
            severity = "ATTENTION"
            msg = "Recent gameplay accuracy has been lower compared with the previous baseline."
        else:
            direction = "STABLE"
            severity = "INFO"
            msg = "Overall gameplay accuracy has remained steady and consistent."

        directions_for_summary.append(direction)
        observations.append(
            PerformanceObservation(
                category="ACCURACY",
                direction=direction,
                severity=severity,
                title="Overall Accuracy",
                message=msg,
                evidence=EvidenceData(
                    recent_average=rec_acc,
                    baseline_average=base_acc,
                    delta=delta_acc,
                    unit="%",
                ),
            )
        )
    else:
        observations.append(
            PerformanceObservation(
                category="ACCURACY",
                direction="NOT_ENOUGH_DATA",
                severity="INFO",
                title="Overall Accuracy",
                message="Not enough gameplay accuracy records available in both periods to calculate a comparative trend.",
                evidence=None,
            )
        )

    # 5. Evaluate Memory Performance
    rec_mem = _safe_float_avg([m.memory_score for m in recent_metrics])
    base_mem = _safe_float_avg([m.memory_score for m in baseline_metrics])

    if rec_mem is not None and base_mem is not None:
        delta_mem = round(rec_mem - base_mem, 1)
        if delta_mem >= MEMORY_SCORE_CHANGE_THRESHOLD:
            direction = "IMPROVING"
            severity = "POSITIVE"
            msg = "Memory game performance has shown notable improvement over recent sessions."
        elif delta_mem <= -MEMORY_SCORE_CHANGE_THRESHOLD:
            direction = "LOWER"
            severity = "ATTENTION"
            msg = "Memory game accuracy has moderated compared with the patient's recent baseline."
        else:
            direction = "STABLE"
            severity = "INFO"
            msg = "Memory game performance has remained consistent across recent sessions."

        directions_for_summary.append(direction)
        observations.append(
            PerformanceObservation(
                category="MEMORY",
                direction=direction,
                severity=severity,
                title="Memory Performance",
                message=msg,
                evidence=EvidenceData(
                    recent_average=rec_mem,
                    baseline_average=base_mem,
                    delta=delta_mem,
                    unit="points",
                ),
            )
        )
    else:
        observations.append(
            PerformanceObservation(
                category="MEMORY",
                direction="NOT_ENOUGH_DATA",
                severity="INFO",
                title="Memory Performance",
                message="Not enough memory game activity recorded in both periods to evaluate a trend.",
                evidence=None,
            )
        )

    # 6. Evaluate Attention & Concentration Performance
    rec_att = _safe_float_avg([m.attention_score for m in recent_metrics])
    base_att = _safe_float_avg([m.attention_score for m in baseline_metrics])

    if rec_att is not None and base_att is not None:
        delta_att = round(rec_att - base_att, 1)
        if delta_att >= ATTENTION_SCORE_CHANGE_THRESHOLD:
            direction = "IMPROVING"
            severity = "POSITIVE"
            msg = "Concentration and attention game scores have improved over recent sessions."
        elif delta_att <= -ATTENTION_SCORE_CHANGE_THRESHOLD:
            direction = "LOWER"
            severity = "ATTENTION"
            msg = "Recent attention game performance has been lower than the previous baseline."
        else:
            direction = "STABLE"
            severity = "INFO"
            msg = "Concentration and attention performance has remained relatively stable."

        directions_for_summary.append(direction)
        observations.append(
            PerformanceObservation(
                category="ATTENTION",
                direction=direction,
                severity=severity,
                title="Attention & Focus",
                message=msg,
                evidence=EvidenceData(
                    recent_average=rec_att,
                    baseline_average=base_att,
                    delta=delta_att,
                    unit="points",
                ),
            )
        )
    else:
        observations.append(
            PerformanceObservation(
                category="ATTENTION",
                direction="NOT_ENOUGH_DATA",
                severity="INFO",
                title="Attention & Focus",
                message="Not enough concentration or attention game activity recorded in both periods to evaluate a trend.",
                evidence=None,
            )
        )

    # 7. Evaluate Response Time
    rec_rt = _safe_float_avg([m.average_response_time_ms for m in recent_metrics])
    base_rt = _safe_float_avg([m.average_response_time_ms for m in baseline_metrics])

    if rec_rt is not None and base_rt is not None:
        delta_rt = round(rec_rt - base_rt, 0)
        if delta_rt <= RESPONSE_TIME_FASTER_THRESHOLD_MS:
            direction = "FASTER"
            severity = "POSITIVE"
            msg = "Average response time has decreased, reflecting quicker task completion in recent games."
        elif delta_rt >= RESPONSE_TIME_SLOWER_THRESHOLD_MS:
            direction = "SLOWER"
            severity = "INFO"
            msg = "Average response time has increased compared with the earlier baseline period."
        else:
            direction = "STABLE"
            severity = "INFO"
            msg = "Response time has remained consistent across recent gameplay."

        directions_for_summary.append(direction)
        observations.append(
            PerformanceObservation(
                category="RESPONSE_TIME",
                direction=direction,
                severity=severity,
                title="Response Speed",
                message=msg,
                evidence=EvidenceData(
                    recent_average=rec_rt,
                    baseline_average=base_rt,
                    delta=delta_rt,
                    unit="ms",
                ),
            )
        )
    else:
        observations.append(
            PerformanceObservation(
                category="RESPONSE_TIME",
                direction="NOT_ENOUGH_DATA",
                severity="INFO",
                title="Response Speed",
                message="Not enough response time telemetry recorded in both periods to evaluate a trend.",
                evidence=None,
            )
        )

    # 8. Evaluate Engagement & Activity
    if recent_games >= 3:
        eng_dir = "STABLE"
        eng_msg = f"Consistent gameplay engagement observed with {recent_games} game session(s) completed across the recent period."
    else:
        eng_dir = "LOWER"
        eng_msg = f"Gameplay activity has been infrequent recently ({recent_games} session(s) completed); regular sessions help maintain reliable tracking."

    observations.append(
        PerformanceObservation(
            category="ENGAGEMENT",
            direction=eng_dir,
            severity="INFO",
            title="Recent Activity",
            message=eng_msg,
            evidence=EvidenceData(
                recent_average=float(recent_games),
                delta=None,
                unit="games completed",
            ),
        )
    )

    # 9. Determine Overall Headline & Primary Direction
    improving_count = sum(1 for d in directions_for_summary if d in ("IMPROVING", "FASTER"))
    lower_count = sum(1 for d in directions_for_summary if d in ("LOWER", "SLOWER"))
    stable_count = sum(1 for d in directions_for_summary if d == "STABLE")

    if not directions_for_summary:
        primary_dir = "NOT_ENOUGH_DATA"
        headline = "Insufficient category data recorded to determine an overall performance trend."
    elif improving_count > lower_count and improving_count >= stable_count:
        primary_dir = "IMPROVING"
        headline = "Recent gameplay shows positive improvement across evaluated activities."
    elif lower_count > improving_count and lower_count >= stable_count:
        primary_dir = "LOWER"
        headline = "Recent gameplay scores have moderated compared with the previous baseline period."
    elif stable_count >= improving_count and stable_count >= lower_count:
        primary_dir = "STABLE"
        headline = "Recent gameplay performance has remained steady and consistent."
    else:
        primary_dir = "VARIABLE"
        headline = "Recent gameplay performance shows varied patterns across different game types."

    return PatientAnalysisResponse(
        patient_id=patient_id,
        generated_at=now,
        data_sufficiency="SUFFICIENT",
        data_window=window_info,
        summary=AnalysisSummary(
            headline=headline,
            primary_direction=primary_dir,
        ),
        observations=observations,
    )
