import { useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  createPatientReminder,
  deletePatientReminder,
  togglePatientReminderStatus,
  updatePatientReminder,
} from "../../api";
import { Icon } from "../Icons";
import PerformanceChart from "../PerformanceChart";
import StatCard from "../StatCard";

export function initials(name = "") {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join("").toUpperCase() || "CC";
}

export function formatDate(value, options = { day: "numeric", month: "short" }) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-IN", options).format(new Date(value));
}

export function formatTime(value) {
  if (!value) return "—";
  try {
    return new Intl.DateTimeFormat("en-IN", {
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    }).format(new Date(value));
  } catch {
    return "—";
  }
}


export function LoadingState({ message = "Loading patient data…" }) {
  return <div className="state-card"><div className="spinner" /><p>{message}</p></div>;
}

export function ErrorState({ message, onRetry }) {
  return <div className="state-card error-state"><strong>We couldn’t load the caregiver workspace</strong><p>{message}</p><button className="button secondary" onClick={onRetry}>Try again</button></div>;
}

export function EmptyPatientsState({ onAddPatient }) {
  return (
    <div className="state-card">
      <strong>No assigned patients yet</strong>
      <p>Your caregiver account does not have any patient assignments.</p>
      {onAddPatient ? (
        <button className="button primary" type="button" onClick={onAddPatient} style={{ marginTop: "10px" }}>
          Add Patient
        </button>
      ) : (
        <Link to="/app/patients?add=true" className="button primary" style={{ marginTop: "10px", textDecoration: "none" }}>
          Add Patient
        </Link>
      )}
    </div>
  );
}

export function PageIntro({ title, eyebrow, patient, action }) {
  return (
    <div className="page-intro">
      <div>
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p>Clear, compassionate insights from cognitive game activity.</p>
      </div>
      {patient ? (
        <div className="patient-chip">
          <div className="avatar patient">{initials(patient.display_name)}</div>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
              <small style={{ margin: 0 }}>Patient</small>
              {patient.public_id && <span className="public-id-badge">{patient.public_id}</span>}
            </div>
            <strong>{patient.display_name}</strong>
          </div>
          {action}
        </div>
      ) : (
        action && <div className="page-intro-action">{action}</div>
      )}
    </div>
  );
}

export function PatientSelector({ patients, selectedId }) {
  const navigate = useNavigate();
  const location = useLocation();
  if (!patients || !patients.length) return null;

  function handleChange(event) {
    const nextId = event.target.value;
    if (!nextId) return;
    if (nextId === "__ADD_PATIENT__") {
      navigate("/app/patients?add=true");
      return;
    }
    const currentPath = location.pathname;
    if (selectedId && currentPath.includes(`/patients/${selectedId}`)) {
      navigate(currentPath.replace(`/patients/${selectedId}`, `/patients/${nextId}`));
    } else {
      navigate(`/app/patients/${nextId}`);
    }
  }

  return (
    <label className="patient-selector">
      <span>Viewing patient</span>
      <select value={selectedId || ""} onChange={handleChange} aria-label="Select patient">
        {patients.map((patient) => (
          <option key={patient.id} value={patient.id}>
            {patient.display_name}
          </option>
        ))}
        <option value="__ADD_PATIENT__">+ Add patient…</option>
      </select>
      <Icon name="chevron" size={16} />
    </label>
  );
}

export function SessionsTable({ sessions, compact = false }) {
  return <div className={`session-list ${compact ? "compact" : ""}`}><div className="list-heading"><span>Recent activity</span><span>{sessions.length} sessions</span></div>{sessions.length ? sessions.map((session) => <div className="session-row" key={session.id}><div className={`game-badge ${session.game_code.toLowerCase()}`}><Icon name="game" size={17} /></div><div className="session-main"><strong>{session.game_name}</strong><span>{formatDate(session.started_at, { day: "numeric", month: "short", year: "numeric" })} · Level {session.difficulty_level}{session.result?.score != null ? ` · Score ${session.result.score}` : ""}{session.result?.correct_answers != null && session.result?.total_questions != null ? ` · ${session.result.correct_answers}/${session.result.total_questions} correct` : ""}</span></div>{session.result ? <div className="session-score"><strong>{session.result.accuracy == null ? "—" : `${Number(session.result.accuracy).toFixed(0)}%`}</strong><span>accuracy</span></div> : <span className="status-pill">{session.status}</span>}{!compact && <div className="session-duration"><Icon name="clock" size={14} />{session.result?.response_time_ms ? `${(session.result.response_time_ms / 1000).toFixed(1)}s` : "—"}</div>}</div>) : <p className="muted">No game sessions yet.</p>}</div>;
}

export function Reminders({
  reminders,
  patientId,
  onCreated,
  isPrimary = false,
  currentCaregiverPublicId = "",
}) {
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({
    title: "",
    reminder_type: "GAME",
    scheduled_at: "",
    is_recurring: false,
    recurrence_rule: "",
  });
  const [state, setState] = useState({ loading: false, error: "", success: "" });

  function startEdit(reminder) {
    setEditingId(reminder.id);
    const d = new Date(reminder.scheduled_at);
    const pad = (n) => String(n).padStart(2, "0");
    const localDateTime = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    setForm({
      title: reminder.title,
      reminder_type: reminder.reminder_type,
      scheduled_at: localDateTime,
      is_recurring: Boolean(reminder.is_recurring),
      recurrence_rule: reminder.recurrence_rule || "",
    });
    setState({ loading: false, error: "", success: "" });
  }

  function cancelEdit() {
    setEditingId(null);
    setForm({
      title: "",
      reminder_type: "GAME",
      scheduled_at: "",
      is_recurring: false,
      recurrence_rule: "",
    });
    setState({ loading: false, error: "", success: "" });
  }

  async function submit(event) {
    event.preventDefault();
    setState({ loading: true, error: "", success: "" });
    try {
      const payload = {
        title: form.title.trim(),
        reminder_type: form.reminder_type,
        scheduled_at: new Date(form.scheduled_at).toISOString(),
        is_recurring: form.is_recurring,
        recurrence_rule: form.is_recurring ? form.recurrence_rule.trim() : null,
      };
      if (editingId) {
        await updatePatientReminder(patientId, editingId, payload);
        setEditingId(null);
        setState({ loading: false, error: "", success: "Reminder updated." });
      } else {
        await createPatientReminder(patientId, payload);
        setState({ loading: false, error: "", success: "Reminder created." });
      }
      setForm({
        title: "",
        reminder_type: "GAME",
        scheduled_at: "",
        is_recurring: false,
        recurrence_rule: "",
      });
      onCreated();
    } catch (error) {
      setState({ loading: false, error: error.message, success: "" });
    }
  }

  async function handleDelete(reminderId) {
    if (!window.confirm("Are you sure you want to delete this reminder?")) return;
    setState({ loading: true, error: "", success: "" });
    try {
      await deletePatientReminder(patientId, reminderId);
      if (editingId === reminderId) {
        cancelEdit();
      }
      setState({ loading: false, error: "", success: "Reminder deleted." });
      onCreated();
    } catch (error) {
      setState({ loading: false, error: error.message, success: "" });
    }
  }

  async function handleToggle(reminder) {
    setState({ loading: true, error: "", success: "" });
    try {
      await togglePatientReminderStatus(patientId, reminder.id, !reminder.is_active);
      setState({
        loading: false,
        error: "",
        success: `Reminder ${reminder.is_active ? "paused" : "activated"}.`,
      });
      onCreated();
    } catch (error) {
      setState({ loading: false, error: error.message, success: "" });
    }
  }

  return (
    <section className="panel reminders-panel">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Stay on track</span>
          <h2>Upcoming reminders</h2>
        </div>
        <Icon name="bell" size={19} />
      </div>

      {reminders.length ? (
        reminders.map((reminder) => {
          const isOwn = Boolean(
            currentCaregiverPublicId &&
              reminder.created_by_caregiver_public_id === currentCaregiverPublicId
          );
          const canManage = isPrimary || isOwn;
          const creatorLabel = reminder.created_by_display_name
            ? `${reminder.created_by_display_name} (${reminder.created_by_caregiver_public_id || "CG"})`
            : reminder.created_by_caregiver_public_id || "Caregiver";

          return (
            <div
              className="reminder-row"
              key={reminder.id}
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                gap: "12px",
              }}
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "11px",
                  minWidth: 0,
                  flex: 1,
                }}
              >
                <div className={`reminder-icon ${reminder.reminder_type.toLowerCase()}`}>
                  <Icon
                    name={reminder.reminder_type === "GAME" ? "game" : "calendar"}
                    size={17}
                  />
                </div>
                <div style={{ minWidth: 0 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                    <strong>{reminder.title}</strong>
                    <span
                      className="status-pill"
                      style={{
                        fontSize: "9px",
                        padding: "2px 7px",
                        background: reminder.is_active ? "#e4f4eb" : "#f0f4f2",
                        color: reminder.is_active ? "#2f7562" : "#81938e",
                      }}
                    >
                      {reminder.is_active ? "Active" : "Inactive"}
                    </span>
                  </div>
                  <span style={{ fontSize: "11px", color: "#8a9c97", marginTop: "2px", display: "block" }}>
                    {formatDate(reminder.scheduled_at, { weekday: "short", day: "numeric", month: "short" })} · {formatDate(reminder.scheduled_at, { hour: "numeric", minute: "2-digit" })}
                    {" · "}
                    Created by {creatorLabel} {isOwn && <strong style={{ display: "inline", color: "#286f5c" }}>(You)</strong>}
                  </span>
                  {reminder.description && (
                    <span style={{ fontSize: "11px", color: "#6b7c77", marginTop: "2px", display: "block" }}>
                      {reminder.description}
                    </span>
                  )}
                </div>
              </div>

              <div style={{ display: "flex", alignItems: "center", gap: "6px", flexShrink: 0 }}>
                {canManage ? (
                  <>
                    <button
                      type="button"
                      className="button secondary"
                      style={{ padding: "5px 8px", fontSize: "10px" }}
                      onClick={() => handleToggle(reminder)}
                      disabled={state.loading}
                      title={reminder.is_active ? "Deactivate reminder" : "Activate reminder"}
                    >
                      {reminder.is_active ? "Pause" : "Resume"}
                    </button>
                    <button
                      type="button"
                      className="button secondary"
                      style={{ padding: "5px 8px", fontSize: "10px" }}
                      onClick={() => startEdit(reminder)}
                      disabled={state.loading}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="button secondary"
                      style={{ padding: "5px 8px", fontSize: "10px", color: "#a45e4f", borderColor: "#f1d5cf" }}
                      onClick={() => handleDelete(reminder.id)}
                      disabled={state.loading}
                    >
                      Delete
                    </button>
                  </>
                ) : (
                  <span style={{ fontSize: "10px", color: "#9daaa7", fontStyle: "italic" }}>
                    View only
                  </span>
                )}
              </div>
            </div>
          );
        })
      ) : (
        <p className="muted">No upcoming reminders.</p>
      )}

      <form className="reminder-form" onSubmit={submit}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "2px" }}>
          <strong style={{ fontSize: "12px", color: "#31574f" }}>
            {editingId ? "Edit reminder" : "Add reminder"}
          </strong>
          {editingId && (
            <button
              type="button"
              className="button secondary"
              style={{ padding: "2px 7px", fontSize: "10px" }}
              onClick={cancelEdit}
            >
              Cancel edit
            </button>
          )}
        </div>
        <div className="form-row">
          <input
            aria-label="Reminder title"
            placeholder="New reminder title"
            value={form.title}
            onChange={(event) => setForm({ ...form, title: event.target.value })}
            required
          />
          <select
            aria-label="Reminder type"
            value={form.reminder_type}
            onChange={(event) => setForm({ ...form, reminder_type: event.target.value })}
          >
            <option value="GAME">GAME</option>
            <option value="ACTIVITY">ACTIVITY</option>
            <option value="APPOINTMENT">APPOINTMENT</option>
            <option value="MEDICATION">MEDICATION</option>
            <option value="OTHER">OTHER</option>
          </select>
        </div>
        <div className="form-row">
          <input
            aria-label="Reminder date and time"
            type="datetime-local"
            value={form.scheduled_at}
            onChange={(event) => setForm({ ...form, scheduled_at: event.target.value })}
            required
          />
          <button className="button secondary" type="submit" disabled={state.loading}>
            {state.loading ? "Saving…" : editingId ? "Update reminder" : "Add reminder"}
          </button>
        </div>
        {state.error && <p className="form-error">{state.error}</p>}
        {state.success && <p className="form-success">{state.success}</p>}
      </form>
    </section>
  );
}

export function PerformanceAnalysisPanel({ analysis }) {
  if (!analysis) return null;

  const isInsufficient = analysis.data_sufficiency === "INSUFFICIENT";
  const windowInfo = analysis.data_window;

  return (
    <section className="panel analysis-panel">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Performance intelligence</span>
          <h2>Rule-based performance observations</h2>
        </div>
        <div className="analysis-window-pill">
          <Icon name="calendar" size={14} />
          {isInsufficient ? (
            <span>{windowInfo?.total_active_days || 0} active day(s) recorded</span>
          ) : (
            <span>
              {formatDate(windowInfo?.recent_start_date)} – {formatDate(windowInfo?.recent_end_date)} ({windowInfo?.recent_days_count} recent vs {windowInfo?.baseline_days_count} baseline days)
            </span>
          )}
        </div>
      </div>

      <div className="analysis-summary-banner">
        <div className="analysis-summary-left">
          <span className={`analysis-dir-pill ${analysis.summary.primary_direction.toLowerCase()}`}>
            {analysis.summary.primary_direction.replace(/_/g, " ")}
          </span>
          <p className="analysis-summary-headline">{analysis.summary.headline}</p>
        </div>
        {windowInfo?.recent_games_completed != null && (
          <div className="analysis-summary-meta">
            <strong>{windowInfo.recent_games_completed}</strong>
            <small>recent games</small>
          </div>
        )}
      </div>

      {isInsufficient ? (
        <div className="insufficient-data-note">
          <p>
            At least 3 active days of gameplay telemetry are needed to calculate a reliable baseline comparison.
            As the patient completes additional game sessions over multiple days, comparative performance trends will be automatically generated.
          </p>
        </div>
      ) : (
        <div className="observations-grid">
          {analysis.observations?.map((obs) => {
            const dirClass = obs.direction.toLowerCase();
            const sevClass = obs.severity.toLowerCase();
            return (
              <div key={obs.category} className={`obs-card ${sevClass}`}>
                <div className="obs-card-header">
                  <span className="obs-cat-title">{obs.title}</span>
                  <span className={`obs-tag ${dirClass}`}>
                    {obs.direction.replace(/_/g, " ")}
                  </span>
                </div>
                <p className="obs-message">{obs.message}</p>
                {obs.evidence && obs.evidence.baseline_average !== null && obs.evidence.delta !== null ? (
                  <div className="obs-evidence">
                    <span className="evidence-metric">
                      Recent: <strong>{obs.evidence.recent_average}{obs.evidence.unit}</strong>
                    </span>
                    <span className="evidence-metric">
                      Baseline: <strong>{obs.evidence.baseline_average}{obs.evidence.unit}</strong>
                    </span>
                    <span className={`evidence-delta ${obs.evidence.delta > 0 ? "positive" : obs.evidence.delta < 0 ? "negative" : "neutral"}`}>
                      {obs.evidence.delta > 0 ? `+${obs.evidence.delta}` : obs.evidence.delta}{obs.evidence.unit}
                    </span>
                  </div>
                ) : obs.evidence?.recent_average != null ? (
                  <div className="obs-evidence">
                    <span className="evidence-metric">
                      Recorded: <strong>{obs.evidence.recent_average} {obs.evidence.unit}</strong>
                    </span>
                  </div>
                ) : null}
              </div>
            );
          })}
        </div>
      )}

      <div className="analysis-disclaimer">
        <small>
          Cogni-Care observations are deterministic, non-clinical heuristics derived from gameplay telemetry.
          They describe cognitive game interactions and do not constitute a medical diagnosis, clinical impairment score, or dementia risk assessment.
        </small>
      </div>
    </section>
  );
}

export function OverviewPatientHero({ patient, relationship, analysis, selectedId, latestMetric }) {
  const isInsufficient = analysis?.data_sufficiency === "INSUFFICIENT";
  const direction = isInsufficient
    ? "INSUFFICIENT_DATA"
    : (analysis?.summary?.primary_direction || "INSUFFICIENT_DATA");
  const headline = analysis?.summary?.headline || (
    isInsufficient
      ? "Performance data is currently limited; at least 3 active days of gameplay are needed to identify reliable trends."
      : "No gameplay performance trend recorded yet."
  );
  const roleLabel = relationship?.is_primary
    ? "Primary Caregiver"
    : relationship?.caregiver_type === "DOCTOR"
      ? "Doctor / Care Team"
      : "Care Team Member";

  return (
    <section className="overview-patient-hero">
      <div className="hero-patient-top">
        <div className="hero-patient-profile">
          <div className="avatar patient large">{initials(patient.display_name)}</div>
          <div className="hero-patient-info">
            <div className="hero-badges">
              <span className="role-pill">{roleLabel}</span>
              {patient.public_id && <span className="public-id-badge">{patient.public_id}</span>}
            </div>
            <h2 className="hero-patient-name">{patient.display_name}</h2>
            <p className="hero-patient-meta">
              Language: <strong>{patient.preferred_language || "English"}</strong> · Timezone: <strong>{patient.timezone || "Local"}</strong>
              {latestMetric?.metric_date && (
                <> · Latest gameplay: <strong>{formatDate(latestMetric.metric_date, { day: "numeric", month: "short", year: "numeric" })}</strong></>
              )}
            </p>
          </div>
        </div>

        <div className="hero-quick-actions">
          <Link className="button secondary" to="/app/patients">
            All patients
          </Link>
          <Link className="button primary" to="/app/patients?add=true">
            + Add Patient
          </Link>
        </div>
      </div>

      <div className="hero-status-strip">
        <div className="hero-status-content">
          <span className={`analysis-dir-pill ${direction.toLowerCase()}`}>
            {direction.replace(/_/g, " ")}
          </span>
          <p className="hero-status-headline">{headline}</p>
        </div>
        <Link className="text-button hero-status-link" to={`/app/patients/${selectedId}/performance`}>
          Detailed analysis <Icon name="arrow" size={14} />
        </Link>
      </div>
    </section>
  );
}

export function UpcomingRemindersPreview({ reminders, patientId }) {
  const activeReminders = (reminders || [])
    .filter((r) => r.is_active)
    .slice(0, 3);

  return (
    <section className="panel overview-preview-panel">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Scheduled care</span>
          <h2>Upcoming reminders</h2>
        </div>
        <Link className="text-button" to={`/app/patients/${patientId}/reminders`}>
          Manage all <Icon name="arrow" size={14} />
        </Link>
      </div>

      {activeReminders.length ? (
        <div className="preview-list">
          {activeReminders.map((reminder) => (
            <div className="preview-row" key={reminder.id}>
              <div className={`reminder-icon ${reminder.reminder_type.toLowerCase()}`}>
                <Icon
                  name={reminder.reminder_type === "GAME" ? "game" : "calendar"}
                  size={16}
                />
              </div>
              <div className="preview-main">
                <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                  <strong>{reminder.title}</strong>
                  <span className="status-pill active" style={{ fontSize: "9px", padding: "1px 6px" }}>
                    {reminder.reminder_type}
                  </span>
                </div>
                <span>
                  {formatDate(reminder.scheduled_at, { weekday: "short", day: "numeric", month: "short" })} · {formatDate(reminder.scheduled_at, { hour: "numeric", minute: "2-digit" })}
                </span>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="preview-empty">
          <p className="muted">No upcoming reminders scheduled.</p>
          <Link to={`/app/patients/${patientId}/reminders`} className="text-button" style={{ marginTop: "6px" }}>
            + Add a reminder
          </Link>
        </div>
      )}
    </section>
  );
}

export function RecentSessionsPreview({ sessions, patientId }) {
  const recentSessions = (sessions || []).slice(0, 4);

  return (
    <section className="panel overview-preview-panel">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Recent activity</span>
          <h2>Latest game sessions</h2>
        </div>
        <Link className="text-button" to={`/app/patients/${patientId}/activity`}>
          View all activity <Icon name="arrow" size={14} />
        </Link>
      </div>

      {recentSessions.length ? (
        <div className="preview-list">
          {recentSessions.map((session) => (
            <div className="preview-row" key={session.id}>
              <div className={`game-badge ${session.game_code.toLowerCase()}`}>
                <Icon name="game" size={16} />
              </div>
              <div className="preview-main">
                <strong>{session.game_name}</strong>
                <span>
                  {formatDate(session.started_at, { day: "numeric", month: "short" })} · Level {session.difficulty_level}
                  {session.result?.response_time_ms ? ` · ${(session.result.response_time_ms / 1000).toFixed(1)}s` : ""}
                </span>
              </div>
              <div className="session-score">
                <strong>{session.result?.accuracy != null ? `${Number(session.result.accuracy).toFixed(0)}%` : "—"}</strong>
                <span>accuracy</span>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="preview-empty">
          <p className="muted">No game sessions recorded yet.</p>
        </div>
      )}
    </section>
  );
}

export function OverviewContent({ data }) {
  const { dashboard, performance, analysis, sessions, reminders, selectedId } = data;
  const latest = dashboard.latest_performance;
  const caregiverFirstName = dashboard.caregiver_relationship?.display_name?.split(" ")[0] || "Caregiver";

  return (
    <div className="overview-workspace">
      <div className="overview-intro-bar">
        <div>
          <span className="eyebrow">Caregiver workspace</span>
          <h1 className="overview-greeting">Welcome back, {caregiverFirstName}</h1>
          <p className="overview-subtitle">
            Caregiver overview & daily status for cognitive support.
          </p>
        </div>
      </div>

      <OverviewPatientHero
        patient={dashboard.patient}
        relationship={dashboard.caregiver_relationship}
        analysis={analysis}
        selectedId={selectedId}
        latestMetric={latest}
      />

      <div className="overview-stats-grid">
        <StatCard
          icon="trend"
          label="Average accuracy"
          value={latest?.average_accuracy == null ? "—" : Number(latest.average_accuracy).toFixed(0)}
          suffix="%"
          tone="blue"
        />
        <StatCard
          icon="clock"
          label="Avg. response speed"
          value={latest?.average_response_time_ms == null ? "—" : (latest.average_response_time_ms / 1000).toFixed(1)}
          suffix="s"
          tone="mint"
        />
        <StatCard
          icon="game"
          label="Completed sessions"
          value={latest?.games_completed ?? "0"}
          suffix={latest?.metric_date ? ` (${formatDate(latest.metric_date)})` : ""}
          tone="peach"
        />
        <StatCard
          icon="users"
          label="Memory performance"
          value={latest?.memory_score == null ? "—" : Number(latest.memory_score).toFixed(0)}
          suffix={latest?.memory_score != null ? "/100" : ""}
          tone="lavender"
        />
      </div>

      <div className="overview-two-col">
        <UpcomingRemindersPreview
          reminders={reminders}
          patientId={selectedId}
        />
        <RecentSessionsPreview
          sessions={sessions}
          patientId={selectedId}
        />
      </div>
    </div>
  );
}

export function PerformanceTelemetryPanel({ latestMetric }) {
  return (
    <section className="panel performance-telemetry-panel">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Supporting telemetry</span>
          <h2>Latest recorded metrics</h2>
        </div>
        {latestMetric?.metric_date && (
          <span className="analysis-window-pill">
            <Icon name="calendar" size={13} />
            Recorded: {formatDate(latestMetric.metric_date, { day: "numeric", month: "short", year: "numeric" })}
          </span>
        )}
      </div>

      <div className="telemetry-metrics-grid">
        <div className="telemetry-card">
          <span className="telemetry-label">Average accuracy</span>
          <strong className="telemetry-value">
            {latestMetric?.average_accuracy == null ? "—" : `${Number(latestMetric.average_accuracy).toFixed(0)}%`}
          </strong>
          <small className="telemetry-sub">Across latest sessions</small>
        </div>

        <div className="telemetry-card">
          <span className="telemetry-label">Avg. response speed</span>
          <strong className="telemetry-value">
            {latestMetric?.average_response_time_ms == null ? "—" : `${(latestMetric.average_response_time_ms / 1000).toFixed(1)}s`}
          </strong>
          <small className="telemetry-sub">Task deliberation pace</small>
        </div>

        <div className="telemetry-card">
          <span className="telemetry-label">Memory performance</span>
          <strong className="telemetry-value">
            {latestMetric?.memory_score == null ? "—" : `${Number(latestMetric.memory_score).toFixed(0)}`}
            {latestMetric?.memory_score != null && <span className="telemetry-denom">/100</span>}
          </strong>
          <small className="telemetry-sub">
            {latestMetric?.memory_score == null ? "No memory games in session" : "Card & pattern recall"}
          </small>
        </div>

        <div className="telemetry-card">
          <span className="telemetry-label">Attention performance</span>
          <strong className="telemetry-value">
            {latestMetric?.attention_score == null ? "—" : `${Number(latestMetric.attention_score).toFixed(0)}`}
            {latestMetric?.attention_score != null && <span className="telemetry-denom">/100</span>}
          </strong>
          <small className="telemetry-sub">
            {latestMetric?.attention_score == null ? "No attention games in session" : "Focus & recognition"}
          </small>
        </div>

        <div className="telemetry-card">
          <span className="telemetry-label">Completed sessions</span>
          <strong className="telemetry-value">
            {latestMetric?.games_completed ?? 0}
          </strong>
          <small className="telemetry-sub">Total daily games</small>
        </div>
      </div>
    </section>
  );
}

export function ActivityBridgePanel({ sessions, patientId }) {
  const recentPreview = (sessions || []).slice(0, 3);
  const totalCount = (sessions || []).length;

  return (
    <section className="panel activity-bridge-panel">
      <div className="bridge-header">
        <div>
          <span className="eyebrow">Detailed game history</span>
          <h2>Session telemetry & activity logs</h2>
          <p className="bridge-desc">
            Review detailed question-by-question metrics, mistakes, and difficulty progression in the dedicated activity workspace.
          </p>
        </div>
        <Link className="button primary bridge-action" to={`/app/patients/${patientId}/activity`}>
          Open activity log ({totalCount} sessions) <Icon name="arrow" size={14} />
        </Link>
      </div>

      {recentPreview.length > 0 && (
        <div className="bridge-recent-preview">
          <span className="bridge-preview-title">Recent session preview:</span>
          <div className="bridge-chips-row">
            {recentPreview.map((session) => (
              <div className="bridge-session-chip" key={session.id}>
                <div className={`game-badge ${session.game_code.toLowerCase()}`} style={{ width: "24px", height: "24px", borderRadius: "7px" }}>
                  <Icon name="game" size={13} />
                </div>
                <div className="bridge-session-text">
                  <strong>{session.game_name}</strong>
                  <span>
                    {formatDate(session.started_at, { day: "numeric", month: "short" })} · {session.result?.accuracy != null ? `${Number(session.result.accuracy).toFixed(0)}%` : "Completed"}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

export function PerformanceContent({ data }) {
  const { dashboard, performance, analysis, sessions, selectedId } = data;
  const games = useMemo(
    () =>
      Object.values(
        sessions.reduce((result, session) => {
          if (!result[session.game_code]) result[session.game_code] = { ...session, attempts: 0, accuracy: 0 };
          result[session.game_code].attempts += 1;
          result[session.game_code].accuracy += Number(session.result?.accuracy || 0);
          return result;
        }, {})
      ).map((game) => ({ ...game, accuracy: game.accuracy / game.attempts })),
    [sessions]
  );
  const latest = dashboard.latest_performance;

  return (
    <div className="performance-workspace">
      {/* 1. Concise Patient & Page Header */}
      <div className="performance-intro-bar">
        <div>
          <span className="eyebrow">Patient performance</span>
          <h1 className="performance-title">Cognitive game performance</h1>
          <p className="performance-subtitle">
            Performance patterns and telemetry for <strong>{dashboard.patient.display_name}</strong>
            {dashboard.patient.public_id && <> ({dashboard.patient.public_id})</>}
          </p>
        </div>
        <Link className="button secondary back-button" to={`/app/patients/${selectedId}`}>
          ← Back to overview
        </Link>
      </div>

      {/* 2. PRIMARY SECTION: Rule-Based Performance Analysis */}
      {analysis && (
        <PerformanceAnalysisPanel analysis={analysis} />
      )}

      {/* 3. Supporting Raw Telemetry */}
      <PerformanceTelemetryPanel latestMetric={latest} />

      {/* 4 & 5. Two-Column Deep-Dive: Historical Trend Curve & Activity Breakdown */}
      <div className="performance-two-col">
        <section className="panel full-chart-panel">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Longitudinal history</span>
              <h2>Performance trend curve</h2>
            </div>
            <span className="chart-context-note">Daily composite (0–100)</span>
          </div>
          <PerformanceChart metrics={performance} />
        </section>

        <section className="panel game-breakdown">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Activity distribution</span>
              <h2>Game-by-game accuracy</h2>
            </div>
          </div>
          <div className="game-table">
            {games.length ? (
              games.map((game) => (
                <div className="game-table-row" key={game.game_code}>
                  <div className={`game-badge ${game.game_code.toLowerCase()}`}>
                    <Icon name="game" size={17} />
                  </div>
                  <div className="game-table-name">
                    <strong>{game.game_name}</strong>
                    <span>{game.attempts} completed · Latest level {game.difficulty_level}</span>
                  </div>
                  <div className="bar-track">
                    <i style={{ width: `${Math.min(game.accuracy, 100)}%` }} />
                  </div>
                  <strong className="game-percent">{game.accuracy.toFixed(0)}%</strong>
                </div>
              ))
            ) : (
              <div className="preview-empty" style={{ padding: "40px 16px" }}>
                <p className="muted">No game performance recorded yet.</p>
              </div>
            )}
          </div>
        </section>
      </div>

      {/* 6. Link to Detailed Activity Log (Replaces redundant 50-row table) */}
      <ActivityBridgePanel sessions={sessions} patientId={selectedId} />
    </div>
  );
}

function formatSessionStatus(status) {
  if (!status) return "—";
  const s = String(status).toUpperCase();
  if (s === "COMPLETED") return "Completed";
  if (s === "IN_PROGRESS") return "In progress";
  if (s === "ABANDONED") return "Incomplete";
  return status;
}

function statusClass(status) {
  if (!status) return "neutral";
  const norm = String(status).toLowerCase();
  if (norm === "completed") return "completed";
  if (norm === "in_progress") return "in_progress";
  if (norm === "abandoned") return "abandoned";
  return "neutral";
}

export function ActivityContent({ data }) {
  const { dashboard, sessions = [], selectedId } = data;
  const patient = dashboard?.patient;

  const [selectedGame, setSelectedGame] = useState("ALL");
  const [sortOrder, setSortOrder] = useState("newest");

  // Summary telemetry strictly calculated from loaded sessions window
  const accuracySessions = useMemo(
    () => sessions.filter((s) => s.result?.accuracy != null && !isNaN(Number(s.result.accuracy))),
    [sessions]
  );
  const windowAvgAccuracy = useMemo(() => {
    if (accuracySessions.length === 0) return null;
    const total = accuracySessions.reduce((sum, s) => sum + Number(s.result.accuracy), 0);
    return Math.round(total / accuracySessions.length);
  }, [accuracySessions]);

  const distinctGamesCount = useMemo(() => {
    return new Set(sessions.map((s) => s.game_code).filter(Boolean)).size;
  }, [sessions]);

  const latestSession = sessions[0];
  const latestDateFormatted = latestSession?.started_at
    ? formatDate(latestSession.started_at, { day: "numeric", month: "short" })
    : null;
  const latestTimeFormatted = latestSession?.started_at
    ? formatTime(latestSession.started_at)
    : null;

  // Game options for filter
  const gameOptions = useMemo(() => {
    const map = new Map();
    for (const s of sessions) {
      const code = s.game_code || "UNKNOWN";
      const name = s.game_name || s.game_code || "Unknown Game";
      if (!map.has(code)) map.set(code, { code, name, count: 0 });
      map.get(code).count += 1;
    }
    return Array.from(map.values());
  }, [sessions]);

  // Filtering & Sorting
  const filteredSessions = useMemo(() => {
    let list = [...sessions];
    if (selectedGame !== "ALL") {
      list = list.filter((s) => s.game_code === selectedGame);
    }
    list.sort((a, b) => {
      const timeA = a.started_at ? new Date(a.started_at).getTime() : 0;
      const timeB = b.started_at ? new Date(b.started_at).getTime() : 0;
      return sortOrder === "oldest" ? timeA - timeB : timeB - timeA;
    });
    return list;
  }, [sessions, selectedGame, sortOrder]);

  const isFiltered = selectedGame !== "ALL" || sortOrder !== "newest";

  const handleResetFilters = () => {
    setSelectedGame("ALL");
    setSortOrder("newest");
  };

  // Date Grouping preserving order
  const dateGroups = useMemo(() => {
    const groups = [];
    const groupMap = new Map();
    for (const session of filteredSessions) {
      const dateKey = session.started_at ? session.started_at.slice(0, 10) : "unknown";
      if (!groupMap.has(dateKey)) {
        const group = {
          dateKey,
          dateFormatted: session.started_at
            ? formatDate(session.started_at, {
                weekday: "short",
                day: "numeric",
                month: "short",
                year: "numeric",
              })
            : "Date unavailable",
          sessions: [],
        };
        groupMap.set(dateKey, group);
        groups.push(group);
      }
      groupMap.get(dateKey).sessions.push(session);
    }
    return groups;
  }, [filteredSessions]);

  return (
    <div className="activity-workspace">
      {/* 1. Page Context / Header */}
      <div className="activity-intro-bar">
        <div>
          <span className="eyebrow">Patient activity</span>
          <h1 className="activity-title">Gameplay activity & session history</h1>
          <p className="activity-subtitle">
            Recorded gameplay sessions and response telemetry for{" "}
            <strong>{patient?.display_name || "Patient"}</strong>
            {patient?.public_id && <> ({patient.public_id})</>}
          </p>
        </div>
        <Link className="button secondary back-button" to={`/app/patients/${selectedId}`}>
          ← Back to overview
        </Link>
      </div>

      {/* 2. Activity Summary Bar (Loaded Window Metrics) */}
      <section className="activity-summary-panel">
        <div className="activity-summary-grid">
          <div className="activity-summary-card">
            <span className="summary-label">Sessions in window</span>
            <strong className="summary-value">{sessions.length}</strong>
            <small className="summary-sub">Up to 50 recent sessions</small>
          </div>

          <div className="activity-summary-card">
            <span className="summary-label">Latest activity</span>
            <strong className="summary-value">
              {latestDateFormatted ? `${latestDateFormatted}, ${latestTimeFormatted}` : "—"}
            </strong>
            <small className="summary-sub">
              {latestSession ? "Most recent session" : "No activity recorded"}
            </small>
          </div>

          <div className="activity-summary-card">
            <span className="summary-label">Window avg. accuracy</span>
            <strong className="summary-value">
              {windowAvgAccuracy != null ? `${windowAvgAccuracy}%` : "—"}
            </strong>
            <small className="summary-sub">
              {windowAvgAccuracy != null
                ? `Across ${accuracySessions.length} evaluated session${accuracySessions.length > 1 ? "s" : ""}`
                : "No accuracy recorded"}
            </small>
          </div>

          <div className="activity-summary-card">
            <span className="summary-label">Distinct games</span>
            <strong className="summary-value">
              {distinctGamesCount > 0 ? distinctGamesCount : "—"}
            </strong>
            <small className="summary-sub">
              {distinctGamesCount > 0 ? "Cognitive activities played" : "No games played"}
            </small>
          </div>
        </div>
      </section>

      {/* 3. Filtering & Sorting Controls Bar */}
      {sessions.length > 0 && (
        <section className="activity-controls-bar">
          <div className="controls-left">
            <div className="control-group">
              <label htmlFor="game-filter" className="control-label">
                Filter by game:
              </label>
              <select
                id="game-filter"
                className="activity-select"
                value={selectedGame}
                onChange={(e) => setSelectedGame(e.target.value)}
              >
                <option value="ALL">All games ({sessions.length})</option>
                {gameOptions.map((g) => (
                  <option key={g.code} value={g.code}>
                    {g.name} ({g.count})
                  </option>
                ))}
              </select>
            </div>

            <div className="control-group">
              <label htmlFor="sort-order" className="control-label">
                Sort by:
              </label>
              <select
                id="sort-order"
                className="activity-select"
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value)}
              >
                <option value="newest">Newest first</option>
                <option value="oldest">Oldest first</option>
              </select>
            </div>

            {isFiltered && (
              <button
                type="button"
                className="text-button activity-reset-btn"
                onClick={handleResetFilters}
              >
                Reset filters
              </button>
            )}
          </div>

          <span className="controls-count-note">
            Showing <strong>{filteredSessions.length}</strong> of {sessions.length} sessions
          </span>
        </section>
      )}

      {/* 4. Session History List (Date-Grouped) */}
      {sessions.length === 0 ? (
        <div className="panel activity-empty-card">
          <div className="empty-icon-wrap">
            <Icon name="game" size={32} />
          </div>
          <h3>No gameplay activity recorded yet</h3>
          <p>
            When {patient?.display_name || "the patient"} plays cognitive games on their mobile app,
            session records, accuracy, and pace will appear here automatically.
          </p>
          {patient?.public_id && (
            <div className="empty-hint-card">
              <span>Patient device code:</span>
              <strong>{patient.public_id}</strong>
            </div>
          )}
        </div>
      ) : filteredSessions.length === 0 ? (
        <div className="panel activity-filter-empty-card">
          <p>No recorded sessions match the selected game filter.</p>
          <button type="button" className="button secondary" onClick={handleResetFilters}>
            Clear filter
          </button>
        </div>
      ) : (
        <div className="activity-session-groups">
          {dateGroups.map((group) => (
            <section key={group.dateKey} className="activity-date-group">
              <div className="date-group-header">
                <div className="date-group-title">
                  <Icon name="calendar" size={15} />
                  <h3>{group.dateFormatted}</h3>
                </div>
                <span className="date-group-count">
                  {group.sessions.length} session{group.sessions.length > 1 ? "s" : ""}
                </span>
              </div>

              <div className="date-group-list">
                {group.sessions.map((session) => {
                  const res = session.result;
                  const accuracy = res?.accuracy != null ? Number(res.accuracy) : null;
                  const paceSec = res?.response_time_ms != null ? (res.response_time_ms / 1000).toFixed(1) : null;
                  const hasQuestions = res?.correct_answers != null && res?.total_questions != null;
                  const mistakes = res?.mistakes != null ? Number(res.mistakes) : null;
                  const score = res?.score != null ? Number(res.score) : null;

                  return (
                    <article key={session.id} className="session-item-card">
                      {/* Left: Game Badge & Identity */}
                      <div className="session-identity">
                        <div
                          className={`game-badge ${session.game_code ? session.game_code.toLowerCase() : ""}`}
                        >
                          <Icon name="game" size={17} />
                        </div>
                        <div className="session-identity-text">
                          <strong className="session-game-name">
                            {session.game_name || session.game_code || "Game session"}
                          </strong>
                          <div className="session-meta-row">
                            <span className="session-time">
                              {session.started_at ? formatTime(session.started_at) : "—"}
                            </span>
                            {session.difficulty_level != null && (
                              <span className="session-difficulty-pill">
                                Level {session.difficulty_level}
                              </span>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* Middle: Key Telemetry */}
                      <div className="session-telemetry-strip">
                        <div className="session-cell accuracy-cell">
                          <span className="cell-label">Accuracy</span>
                          <strong className="cell-value">
                            {accuracy != null ? `${accuracy.toFixed(0)}%` : "—"}
                          </strong>
                          {accuracy != null && (
                            <div className="mini-accuracy-bar">
                              <i style={{ width: `${Math.min(Math.max(accuracy, 0), 100)}%` }} />
                            </div>
                          )}
                        </div>

                        <div className="session-cell questions-cell">
                          <span className="cell-label">Score / Questions</span>
                          <strong className="cell-value">
                            {hasQuestions
                              ? `${res.correct_answers}/${res.total_questions}`
                              : score != null
                                ? `${score.toFixed(0)} pts`
                                : "—"}
                          </strong>
                          <span className="cell-sub">
                            {hasQuestions
                              ? mistakes != null && mistakes > 0
                                ? `${mistakes} mistake${mistakes > 1 ? "s" : ""}`
                                : "All correct"
                              : score != null
                                ? "Score recorded"
                                : "No question telemetry"}
                          </span>
                        </div>

                        <div className="session-cell pace-cell">
                          <span className="cell-label">Avg. pace</span>
                          <strong className="cell-value">
                            {paceSec != null ? `${paceSec}s` : "—"}
                          </strong>
                          <span className="cell-sub">
                            {paceSec != null ? "Task response pace" : "Pace unrecorded"}
                          </span>
                        </div>
                      </div>

                      {/* Right: Actual Session Status */}
                      <div className="session-status-col">
                        <span
                          className={`status-pill ${statusClass(session.status)}`}
                        >
                          {formatSessionStatus(session.status)}
                        </span>
                      </div>
                    </article>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}

export function RemindersContent({ data }) {
  const { dashboard, reminders = [], selectedId, reload } = data;
  const patient = dashboard?.patient;
  const isPrimary = Boolean(dashboard?.caregiver_relationship?.is_primary);
  const currentCaregiverPublicId = dashboard?.caregiver_relationship?.public_id || "";

  // Modal / Form state
  const [modalOpen, setModalOpen] = useState(false);
  const [editingReminder, setEditingReminder] = useState(null);
  const [form, setForm] = useState({
    title: "",
    description: "",
    reminder_type: "GAME",
    scheduled_at: "",
    is_recurring: false,
    recurrence_rule: "",
  });
  const [actionState, setActionState] = useState({ loading: false, error: "", success: "" });

  // Filtering state
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");

  // Summary indicators
  const activeCount = useMemo(() => reminders.filter((r) => r.is_active).length, [reminders]);
  const recurringCount = useMemo(() => reminders.filter((r) => r.is_recurring).length, [reminders]);

  const todayStr = useMemo(() => {
    const d = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }, []);

  const todayCount = useMemo(() => {
    return reminders.filter((r) => r.scheduled_at && r.scheduled_at.slice(0, 10) === todayStr).length;
  }, [reminders, todayStr]);

  // Open modal for new reminder
  const handleOpenCreate = () => {
    const now = new Date();
    now.setHours(now.getHours() + 1, 0, 0, 0);
    const pad = (n) => String(n).padStart(2, "0");
    const defaultDateTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;

    setEditingReminder(null);
    setForm({
      title: "",
      description: "",
      reminder_type: "GAME",
      scheduled_at: defaultDateTime,
      is_recurring: false,
      recurrence_rule: "",
    });
    setActionState({ loading: false, error: "", success: "" });
    setModalOpen(true);
  };

  // Open modal for editing existing reminder
  const handleOpenEdit = (reminder) => {
    const d = new Date(reminder.scheduled_at);
    const pad = (n) => String(n).padStart(2, "0");
    const localDateTime = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;

    setEditingReminder(reminder);
    setForm({
      title: reminder.title || "",
      description: reminder.description || "",
      reminder_type: reminder.reminder_type || "GAME",
      scheduled_at: localDateTime,
      is_recurring: Boolean(reminder.is_recurring),
      recurrence_rule: reminder.recurrence_rule || "",
    });
    setActionState({ loading: false, error: "", success: "" });
    setModalOpen(true);
  };

  const handleCloseModal = () => {
    setModalOpen(false);
    setEditingReminder(null);
    setActionState({ loading: false, error: "", success: "" });
  };

  // Submit create or edit
  const handleSubmitForm = async (e) => {
    e.preventDefault();
    setActionState({ loading: true, error: "", success: "" });
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim() || null,
        reminder_type: form.reminder_type,
        scheduled_at: new Date(form.scheduled_at).toISOString(),
        is_recurring: Boolean(form.is_recurring),
        recurrence_rule: form.is_recurring ? (form.recurrence_rule.trim() || "Daily") : null,
      };

      if (editingReminder) {
        await updatePatientReminder(selectedId, editingReminder.id, payload);
        setActionState({ loading: false, error: "", success: "Reminder updated successfully." });
      } else {
        await createPatientReminder(selectedId, payload);
        setActionState({ loading: false, error: "", success: "Reminder created successfully." });
      }

      await reload();
      setTimeout(() => {
        handleCloseModal();
      }, 500);
    } catch (err) {
      setActionState({ loading: false, error: err.message, success: "" });
    }
  };

  // Toggle active status (Pause / Resume)
  const handleToggleStatus = async (reminder) => {
    setActionState({ loading: true, error: "", success: "" });
    try {
      await togglePatientReminderStatus(selectedId, reminder.id, !reminder.is_active);
      await reload();
      setActionState({
        loading: false,
        error: "",
        success: `Reminder ${reminder.is_active ? "paused" : "resumed"}.`,
      });
    } catch (err) {
      setActionState({ loading: false, error: err.message, success: "" });
    }
  };

  // Delete reminder
  const handleDeleteReminder = async (reminderId) => {
    if (!window.confirm("Are you sure you want to delete this reminder?")) return;
    setActionState({ loading: true, error: "", success: "" });
    try {
      await deletePatientReminder(selectedId, reminderId);
      if (editingReminder?.id === reminderId) {
        handleCloseModal();
      }
      await reload();
      setActionState({ loading: false, error: "", success: "Reminder deleted." });
    } catch (err) {
      setActionState({ loading: false, error: err.message, success: "" });
    }
  };

  // Filtered Reminders
  const filteredReminders = useMemo(() => {
    return reminders.filter((r) => {
      if (typeFilter !== "ALL" && r.reminder_type !== typeFilter) return false;
      if (statusFilter === "ACTIVE" && !r.is_active) return false;
      if (statusFilter === "PAUSED" && r.is_active) return false;
      return true;
    });
  }, [reminders, typeFilter, statusFilter]);

  // Temporal Grouping (Today, Tomorrow, Upcoming, Past Scheduled)
  const temporalGroups = useMemo(() => {
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dayAfterTomorrow = new Date(today);
    dayAfterTomorrow.setDate(dayAfterTomorrow.getDate() + 2);

    const groups = {
      today: { id: "today", title: "Today", subtitle: "Scheduled for today's routine", items: [] },
      tomorrow: { id: "tomorrow", title: "Tomorrow", subtitle: "Scheduled for tomorrow", items: [] },
      upcoming: { id: "upcoming", title: "Upcoming", subtitle: "Scheduled for later dates", items: [] },
      past: { id: "past", title: "Past Scheduled", subtitle: "Scheduled time has passed · Reminders are not marked completed", items: [] },
    };

    for (const reminder of filteredReminders) {
      const remDate = new Date(reminder.scheduled_at);
      if (remDate < now && remDate < today) {
        groups.past.items.push(reminder);
      } else if (remDate >= today && remDate < tomorrow) {
        groups.today.items.push(reminder);
      } else if (remDate >= tomorrow && remDate < dayAfterTomorrow) {
        groups.tomorrow.items.push(reminder);
      } else if (remDate >= dayAfterTomorrow) {
        groups.upcoming.items.push(reminder);
      } else {
        // Earlier today
        groups.today.items.push(reminder);
      }
    }

    return Object.values(groups).filter((g) => g.items.length > 0);
  }, [filteredReminders]);

  const isFiltered = typeFilter !== "ALL" || statusFilter !== "ALL";

  const handleResetFilters = () => {
    setTypeFilter("ALL");
    setStatusFilter("ALL");
  };

  return (
    <div className="reminders-workspace">
      {/* 1. Page Context / Workspace Header */}
      <div className="reminders-intro-bar">
        <div>
          <span className="eyebrow">Patient support</span>
          <h1 className="reminders-title">Care reminders & scheduled routines</h1>
          <p className="reminders-subtitle">
            Scheduled cognitive activities, appointments, and routines for{" "}
            <strong>{patient?.display_name || "Patient"}</strong>
            {patient?.public_id && <> ({patient.public_id})</>}
          </p>
        </div>
        <div className="reminders-header-actions">
          <button type="button" className="button primary add-reminder-btn" onClick={handleOpenCreate}>
            <Icon name="bell" size={15} /> Add reminder
          </button>
          <Link className="button secondary back-button" to={`/app/patients/${selectedId}`}>
            ← Back to overview
          </Link>
        </div>
      </div>

      {/* Action feedback notifications */}
      {actionState.error && <div className="notice-box error-notice"><p>{actionState.error}</p></div>}
      {actionState.success && <div className="notice-box success-notice"><p>{actionState.success}</p></div>}

      {/* 2. Reminders Summary Bar */}
      <section className="reminders-summary-panel">
        <div className="reminders-summary-grid">
          <div className="reminders-summary-card">
            <span className="summary-label">Active reminders</span>
            <strong className="summary-value">{activeCount}</strong>
            <small className="summary-sub">
              {reminders.length > 0 ? `Out of ${reminders.length} total reminders` : "No reminders set"}
            </small>
          </div>

          <div className="reminders-summary-card">
            <span className="summary-label">Scheduled today</span>
            <strong className="summary-value">{todayCount}</strong>
            <small className="summary-sub">
              {todayCount > 0 ? "Due today on patient device" : "No reminders due today"}
            </small>
          </div>

          <div className="reminders-summary-card">
            <span className="summary-label">Recurring routines</span>
            <strong className="summary-value">{recurringCount}</strong>
            <small className="summary-sub">
              {recurringCount > 0 ? "Daily or weekly scheduled" : "Single-event reminders"}
            </small>
          </div>

          <div className="reminders-summary-card">
            <span className="summary-label">Permission tier</span>
            <strong className="summary-value role-tier-value">
              {isPrimary ? "Primary Caregiver" : "Care Team"}
            </strong>
            <small className="summary-sub">
              {isPrimary ? "Full management access" : "Manage your own reminders"}
            </small>
          </div>
        </div>
      </section>

      {/* 3. Filtering & View Controls Bar */}
      {reminders.length > 0 && (
        <section className="reminders-controls-bar">
          <div className="controls-left">
            <div className="control-group">
              <label htmlFor="reminder-type-filter" className="control-label">
                Category:
              </label>
              <select
                id="reminder-type-filter"
                className="activity-select"
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
              >
                <option value="ALL">All categories ({reminders.length})</option>
                <option value="GAME">Game</option>
                <option value="ACTIVITY">Activity</option>
                <option value="APPOINTMENT">Appointment</option>
                <option value="MEDICATION">Medication</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div className="control-group">
              <label htmlFor="reminder-status-filter" className="control-label">
                Status:
              </label>
              <select
                id="reminder-status-filter"
                className="activity-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">All statuses</option>
                <option value="ACTIVE">Active only</option>
                <option value="PAUSED">Paused only</option>
              </select>
            </div>

            {isFiltered && (
              <button
                type="button"
                className="text-button activity-reset-btn"
                onClick={handleResetFilters}
              >
                Reset filters
              </button>
            )}
          </div>

          <span className="controls-count-note">
            Showing <strong>{filteredReminders.length}</strong> of {reminders.length} reminders
          </span>
        </section>
      )}

      {/* 4. Reminder Groups / Cards */}
      {reminders.length === 0 ? (
        <div className="panel activity-empty-card">
          <div className="empty-icon-wrap">
            <Icon name="bell" size={32} />
          </div>
          <h3>No reminders scheduled yet</h3>
          <p>
            Create care reminders for cognitive games, daily activities, medications, or appointments.
            Reminders will appear on {patient?.display_name ? `${patient.display_name}’s` : "the patient's"} device automatically.
          </p>
          <button type="button" className="button primary" onClick={handleOpenCreate} style={{ marginTop: "8px" }}>
            <Icon name="bell" size={14} /> Schedule first reminder
          </button>
        </div>
      ) : filteredReminders.length === 0 ? (
        <div className="panel activity-filter-empty-card">
          <p>No reminders match the selected filters.</p>
          <button type="button" className="button secondary" onClick={handleResetFilters}>
            Clear filters
          </button>
        </div>
      ) : (
        <div className="reminders-groups">
          {temporalGroups.map((group) => (
            <section key={group.id} className="reminders-temporal-group">
              <div className="date-group-header">
                <div className="date-group-title">
                  <Icon name="calendar" size={15} />
                  <h3>{group.title}</h3>
                  <span className="group-context-sub">{group.subtitle}</span>
                </div>
                <span className="date-group-count">
                  {group.items.length} reminder{group.items.length > 1 ? "s" : ""}
                </span>
              </div>

              <div className="reminders-card-list">
                {group.items.map((reminder) => {
                  const isOwn = Boolean(
                    currentCaregiverPublicId &&
                      reminder.created_by_caregiver_public_id === currentCaregiverPublicId
                  );
                  const canManage = isPrimary || isOwn;
                  const creatorLabel = reminder.created_by_display_name
                    ? `${reminder.created_by_display_name}`
                    : reminder.created_by_caregiver_public_id
                      ? `Caregiver ${reminder.created_by_caregiver_public_id}`
                      : "Care team";

                  const typeLower = reminder.reminder_type ? reminder.reminder_type.toLowerCase() : "other";

                  return (
                    <article
                      key={reminder.id}
                      className={`reminder-item-card ${!reminder.is_active ? "is-paused" : ""}`}
                    >
                      {/* Left: Type Icon & Main Text */}
                      <div className="reminder-card-main">
                        <div className={`reminder-icon ${typeLower}`}>
                          <Icon
                            name={reminder.reminder_type === "GAME" ? "game" : "calendar"}
                            size={18}
                          />
                        </div>

                        <div className="reminder-text-block">
                          <div className="reminder-title-row">
                            <strong className="reminder-title">{reminder.title}</strong>
                            <span className={`status-pill ${reminder.is_active ? "active" : "paused"}`}>
                              {reminder.is_active ? "Active" : "Paused"}
                            </span>
                          </div>

                          {reminder.description && (
                            <p className="reminder-desc">{reminder.description}</p>
                          )}

                          <div className="reminder-meta-row">
                            <span className="meta-item time">
                              <Icon name="clock" size={13} />
                              {formatDate(reminder.scheduled_at, {
                                weekday: "short",
                                day: "numeric",
                                month: "short",
                              })}{" "}
                              at {formatTime(reminder.scheduled_at)}
                            </span>

                            {reminder.is_recurring && (
                              <span className="meta-item recurring-pill">
                                ↺ {reminder.recurrence_rule || "Recurring"}
                              </span>
                            )}

                            <span className="meta-item type-tag">
                              {reminder.reminder_type}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Right: Creator Attribution & Permission-Aware Actions */}
                      <div className="reminder-card-actions-col">
                        <div className="creator-badge-wrap">
                          {isOwn ? (
                            <span className="creator-pill own" title="Created by your caregiver account">
                              Created by you
                            </span>
                          ) : (
                            <span
                              className="creator-pill team"
                              title={`Created by ${creatorLabel}`}
                            >
                              By {creatorLabel}
                            </span>
                          )}
                        </div>

                        <div className="card-buttons-row">
                          {canManage ? (
                            <>
                              <button
                                type="button"
                                className="button secondary reminder-action-btn"
                                onClick={() => handleToggleStatus(reminder)}
                                disabled={actionState.loading}
                                title={reminder.is_active ? "Pause this reminder" : "Resume this reminder"}
                              >
                                {reminder.is_active ? "Pause" : "Resume"}
                              </button>
                              <button
                                type="button"
                                className="button secondary reminder-action-btn"
                                onClick={() => handleOpenEdit(reminder)}
                                disabled={actionState.loading}
                              >
                                Edit
                              </button>
                              <button
                                type="button"
                                className="button secondary reminder-action-btn delete"
                                onClick={() => handleDeleteReminder(reminder.id)}
                                disabled={actionState.loading}
                              >
                                Delete
                              </button>
                            </>
                          ) : (
                            <span className="view-only-badge" title="Only the creator or primary caregiver can modify this reminder">
                              View only
                            </span>
                          )}
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      )}

      {/* 5. Create / Edit Reminder Modal Dialog */}
      {modalOpen && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal-card reminder-modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <span className="eyebrow">
                  {editingReminder ? "Edit schedule" : "New schedule"}
                </span>
                <h2>{editingReminder ? "Edit care reminder" : "Add care reminder"}</h2>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={handleCloseModal}
                aria-label="Close"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitForm} className="modal-form">
              <label>
                Reminder title *
                <input
                  type="text"
                  placeholder="e.g. Morning Memory Quiz or Dr. Sharma visit"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  required
                  maxLength={150}
                />
              </label>

              <div className="form-two-col">
                <label>
                  Category *
                  <select
                    value={form.reminder_type}
                    onChange={(e) => setForm({ ...form, reminder_type: e.target.value })}
                  >
                    <option value="GAME">GAME (Cognitive play)</option>
                    <option value="ACTIVITY">ACTIVITY (Daily task)</option>
                    <option value="APPOINTMENT">APPOINTMENT (Doctor visit)</option>
                    <option value="MEDICATION">MEDICATION (Medicine)</option>
                    <option value="OTHER">OTHER</option>
                  </select>
                </label>

                <label>
                  Scheduled date & time *
                  <input
                    type="datetime-local"
                    value={form.scheduled_at}
                    onChange={(e) => setForm({ ...form, scheduled_at: e.target.value })}
                    required
                  />
                </label>
              </div>

              <label>
                Description / Notes (optional)
                <input
                  type="text"
                  placeholder="e.g. Assist with pattern recall level 2"
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  maxLength={255}
                />
              </label>

              <div className="recurrence-box">
                <label className="check-label">
                  <input
                    type="checkbox"
                    checked={form.is_recurring}
                    onChange={(e) =>
                      setForm({
                        ...form,
                        is_recurring: e.target.checked,
                        recurrence_rule: e.target.checked ? (form.recurrence_rule || "Daily") : "",
                      })
                    }
                  />
                  <span>Repeat this reminder automatically</span>
                </label>

                {form.is_recurring && (
                  <div className="recurrence-input-row">
                    <label>
                      Recurrence frequency
                      <select
                        value={form.recurrence_rule}
                        onChange={(e) => setForm({ ...form, recurrence_rule: e.target.value })}
                      >
                        <option value="Daily">Daily</option>
                        <option value="Weekly">Weekly</option>
                        <option value="Weekdays (Mon-Fri)">Weekdays (Mon-Fri)</option>
                        <option value="Every 2 days">Every 2 days</option>
                      </select>
                    </label>
                  </div>
                )}
              </div>

              {actionState.error && <p className="form-error">{actionState.error}</p>}
              {actionState.success && <p className="form-success">{actionState.success}</p>}

              <div className="modal-footer">
                <button
                  type="button"
                  className="button secondary"
                  onClick={handleCloseModal}
                  disabled={actionState.loading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="button primary"
                  disabled={actionState.loading}
                >
                  {actionState.loading
                    ? "Saving…"
                    : editingReminder
                      ? "Update reminder"
                      : "Create reminder"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export function formatCaregiverType(type) {
  switch (type) {
    case "FAMILY":
      return "Family Caregiver";
    case "DOCTOR":
      return "Doctor / Specialist";
    case "PROFESSIONAL_CAREGIVER":
      return "Professional Caregiver";
    case "OTHER":
    default:
      return "Caregiver";
  }
}

export function CareTeamContent({ data }) {
  const { dashboard, careTeam = [], selectedId } = data;
  const patient = dashboard?.patient;
  const isPrimary = Boolean(dashboard?.caregiver_relationship?.is_primary);
  const currentCaregiverPublicId = dashboard?.caregiver_relationship?.public_id || "";

  // Add Caregiver Modal state
  const [showAddModal, setShowAddModal] = useState(false);
  const [caregiverPublicId, setCaregiverPublicId] = useState("");
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState("");

  // Confirmation state: { type: "transfer" | "remove", member: object } | null
  const [confirmDialog, setConfirmDialog] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");

  const primaryCaregiver = useMemo(() => careTeam.find((m) => m.is_primary), [careTeam]);
  const secondaryCaregivers = useMemo(() => careTeam.filter((m) => !m.is_primary), [careTeam]);

  // Handle Add Caregiver
  async function handleAddCaregiver(event) {
    event.preventDefault();
    if (!caregiverPublicId.trim()) return;
    setModalLoading(true);
    setModalError("");
    try {
      await data.addCareTeamMember(caregiverPublicId.trim().toUpperCase());
      setShowAddModal(false);
      setCaregiverPublicId("");
      setActionSuccess("Caregiver added to care team successfully.");
      setTimeout(() => setActionSuccess(""), 4000);
    } catch (err) {
      setModalError(err.message || "Failed to add caregiver");
    } finally {
      setModalLoading(false);
    }
  }

  // Handle Transfer or Remove Confirmation
  async function handleConfirmAction() {
    if (!confirmDialog) return;
    setActionLoading(true);
    setActionError("");
    try {
      if (confirmDialog.type === "transfer") {
        await data.transferPrimary(confirmDialog.member.caregiver_id);
        setActionSuccess(`Primary caregiver responsibility transferred to ${confirmDialog.member.display_name}.`);
      } else if (confirmDialog.type === "remove") {
        await data.removeCareTeamMember(confirmDialog.member.caregiver_id);
        setActionSuccess(`${confirmDialog.member.display_name} removed from care team.`);
      }
      setConfirmDialog(null);
      setTimeout(() => setActionSuccess(""), 4000);
    } catch (err) {
      setActionError(err.message || "Action failed");
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div className="care-team-workspace">
      {/* 1. Page Context / Workspace Header */}
      <div className="care-team-intro-bar">
        <div>
          <span className="eyebrow">Patient access & support</span>
          <h1 className="care-team-title">Authorized care team</h1>
          <p className="care-team-subtitle">
            Caregivers and family members with authorized access to{" "}
            <strong>{patient?.display_name || "Patient"}</strong>
            {patient?.public_id && <> ({patient.public_id})</>}
          </p>
        </div>
        <div className="care-team-header-actions">
          {isPrimary && (
            <button
              type="button"
              className="button primary add-caregiver-btn"
              onClick={() => {
                setShowAddModal(true);
                setModalError("");
              }}
            >
              <Icon name="users" size={15} /> Add caregiver
            </button>
          )}
          <Link className="button secondary back-button" to={`/app/patients/${selectedId}`}>
            ← Back to overview
          </Link>
        </div>
      </div>

      {/* Action feedback notifications */}
      {actionError && <div className="notice-box error-notice"><p>{actionError}</p></div>}
      {actionSuccess && <div className="notice-box success-notice"><p>{actionSuccess}</p></div>}

      {/* 2. Summary & Role Context Bar */}
      <section className="care-team-summary-panel">
        <div className="care-team-summary-grid">
          <div className="care-team-summary-card">
            <span className="summary-label">Team size</span>
            <strong className="summary-value">{careTeam.length}</strong>
            <small className="summary-sub">
              {careTeam.length === 1 ? "1 authorized caregiver" : `${careTeam.length} authorized caregivers`}
            </small>
          </div>

          <div className="care-team-summary-card">
            <span className="summary-label">Primary caregiver</span>
            <strong className="summary-value team-primary-name">
              {primaryCaregiver?.display_name || "None designated"}
            </strong>
            <small className="summary-sub">
              {primaryCaregiver ? formatCaregiverType(primaryCaregiver.caregiver_type) : "Awaiting assignment"}
            </small>
          </div>

          <div className="care-team-summary-card">
            <span className="summary-label">Your relationship</span>
            <strong className="summary-value role-tier-value">
              {isPrimary ? "Primary Caregiver" : "Care Team Member"}
            </strong>
            <small className="summary-sub">
              {isPrimary ? "Full caregiver management access" : "Secondary patient access"}
            </small>
          </div>

          <div className="care-team-summary-card">
            <span className="summary-label">Patient code</span>
            <strong className="summary-value patient-code-value">
              {patient?.public_id || "—"}
            </strong>
            <small className="summary-sub">Shared with care team</small>
          </div>
        </div>
      </section>

      {/* Non-primary explanation note */}
      {!isPrimary && (
        <div className="notice-box info-box">
          <Icon name="users" size={16} />
          <p>
            You are connected as a <strong>Secondary Caregiver</strong>. You have access to view this patient&apos;s
            activity, overview, and reminders. Only the Primary Caregiver can add new caregivers, remove members, or
            transfer primary responsibility.
          </p>
        </div>
      )}

      {/* 3. Primary Caregiver Showcase Card */}
      <section className="primary-caregiver-section">
        <div className="section-label-bar">
          <span className="eyebrow">Primary Caregiver</span>
          <span className="section-badge-pill">Accountable Lead</span>
        </div>

        {primaryCaregiver ? (
          <div className="primary-caregiver-card">
            <div className="primary-left">
              <div className="avatar doctor large primary-avatar">
                {initials(primaryCaregiver.display_name)}
              </div>
              <div className="primary-info">
                <div className="primary-name-row">
                  <h2 className="primary-name">{primaryCaregiver.display_name}</h2>
                  {primaryCaregiver.public_id && (
                    <span className="public-id-badge">{primaryCaregiver.public_id}</span>
                  )}
                  {primaryCaregiver.public_id === currentCaregiverPublicId && (
                    <span className="creator-pill own">You</span>
                  )}
                </div>
                <p className="primary-type">{formatCaregiverType(primaryCaregiver.caregiver_type)}</p>
                <p className="primary-note">
                  Holds primary administrative and caregiving responsibility for {patient?.display_name || "this patient"}.
                  Can manage authorized caregivers, reminders, and patient settings.
                </p>
              </div>
            </div>

            <div className="primary-right">
              <span className="role-status-badge primary-badge">
                ★ Primary Caregiver
              </span>
            </div>
          </div>
        ) : (
          <div className="panel empty-primary-card">
            <p>No primary caregiver currently assigned to this patient.</p>
          </div>
        )}
      </section>

      {/* 4. Secondary Caregivers Section */}
      <section className="secondary-caregivers-section">
        <div className="section-label-bar">
          <div>
            <span className="eyebrow">Care Team Members</span>
            <h2 className="secondary-section-title">Secondary caregivers ({secondaryCaregivers.length})</h2>
          </div>
          <span className="secondary-desc">
            Authorized family members, specialists, and caregivers supporting this patient
          </span>
        </div>

        {secondaryCaregivers.length === 0 ? (
          <div className="panel secondary-empty-card">
            <div className="empty-icon-wrap">
              <Icon name="users" size={28} />
            </div>
            <h3>No secondary caregivers assigned</h3>
            <p>
              {patient?.display_name || "This patient"} currently has only the primary caregiver assigned.
              {isPrimary && " You can invite family members, doctors, or professional caregivers using their Caregiver Public ID."}
            </p>
            {isPrimary && (
              <button
                type="button"
                className="button primary"
                onClick={() => {
                  setShowAddModal(true);
                  setModalError("");
                }}
                style={{ marginTop: "10px" }}
              >
                <Icon name="users" size={14} /> Add Caregiver
              </button>
            )}
          </div>
        ) : (
          <div className="secondary-caregiver-grid">
            {secondaryCaregivers.map((member) => {
              const isCurrentUser = Boolean(
                currentCaregiverPublicId && member.public_id === currentCaregiverPublicId
              );

              return (
                <article key={member.caregiver_id} className="secondary-caregiver-card">
                  <div className="card-top-row">
                    <div className="member-avatar-wrap">
                      <div className="avatar patient member-avatar">
                        {initials(member.display_name)}
                      </div>
                      <div className="member-details">
                        <div className="member-name-row">
                          <strong className="member-name">{member.display_name}</strong>
                          {isCurrentUser && <span className="creator-pill own">You</span>}
                        </div>
                        <span className="member-type">
                          {formatCaregiverType(member.caregiver_type)}
                        </span>
                        {member.public_id && (
                          <span className="public-id-badge member-pub-id">{member.public_id}</span>
                        )}
                      </div>
                    </div>

                    <span className="role-status-badge secondary-badge">
                      Secondary Caregiver
                    </span>
                  </div>

                  {/* Actions for Primary Caregiver */}
                  {isPrimary && (
                    <div className="card-action-bar">
                      <button
                        type="button"
                        className="button secondary transfer-btn"
                        onClick={() => {
                          setActionError("");
                          setConfirmDialog({ type: "transfer", member });
                        }}
                        title={`Transfer primary caregiver responsibility to ${member.display_name}`}
                      >
                        Make Primary
                      </button>
                      <button
                        type="button"
                        className="danger-button remove-btn"
                        onClick={() => {
                          setActionError("");
                          setConfirmDialog({ type: "remove", member });
                        }}
                        title={`Remove ${member.display_name} from care team`}
                      >
                        Remove
                      </button>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>

      {/* Add Caregiver Modal Dialog */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)} role="dialog" aria-modal="true">
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <span className="eyebrow">Care Team Access</span>
                <h2>Add Caregiver</h2>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setShowAddModal(false)}
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleAddCaregiver} className="modal-form">
              <div className="info-callout">
                <p>
                  <strong>How Caregiver Linking Works:</strong> Each registered caregiver has a unique Public ID
                  (e.g., <code>CG-XXXXXXXX</code>). Enter their ID below to grant them secondary access to view
                  this patient&apos;s activity, trends, and reminders.
                </p>
              </div>

              <label>
                Caregiver Public ID *
                <input
                  type="text"
                  value={caregiverPublicId}
                  onChange={(e) => setCaregiverPublicId(e.target.value.toUpperCase())}
                  placeholder="CG-XXXXXXXX"
                  maxLength={30}
                  required
                />
              </label>

              {modalError && (
                <p className="form-error" role="alert">
                  {modalError}
                </p>
              )}

              <div className="modal-footer">
                <button
                  type="button"
                  className="button secondary"
                  onClick={() => setShowAddModal(false)}
                  disabled={modalLoading}
                >
                  Cancel
                </button>
                <button type="submit" className="button primary" disabled={modalLoading}>
                  {modalLoading ? "Adding…" : "Add Caregiver"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirmation Dialog Modal */}
      {confirmDialog && (
        <div className="modal-overlay" onClick={() => setConfirmDialog(null)} role="dialog" aria-modal="true">
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div>
                <span className="eyebrow">Confirmation Required</span>
                <h2>
                  {confirmDialog.type === "transfer"
                    ? "Transfer Primary Responsibility"
                    : "Remove Caregiver"}
                </h2>
              </div>
              <button
                type="button"
                className="modal-close-button"
                onClick={() => setConfirmDialog(null)}
                aria-label="Close dialog"
              >
                ✕
              </button>
            </div>

            <div className="confirm-body" style={{ fontSize: "13px", color: "#4f6963", lineHeight: 1.6, margin: "10px 0 16px" }}>
              {confirmDialog.type === "transfer" ? (
                <>
                  <p>
                    Are you sure you want to transfer primary caregiver responsibility for{" "}
                    <strong>{patient?.display_name}</strong> to{" "}
                    <strong>{confirmDialog.member.display_name}</strong> ({confirmDialog.member.public_id})?
                  </p>
                  <div className="notice-box error-notice" style={{ marginTop: "12px" }}>
                    <p>
                      <strong>Important:</strong> You will become a secondary caregiver and will no longer have authority to
                      manage caregiver access, add team members, or transfer responsibility.
                    </p>
                  </div>
                </>
              ) : (
                <>
                  <p>
                    Are you sure you want to remove{" "}
                    <strong>{confirmDialog.member.display_name}</strong> ({confirmDialog.member.public_id}) from the care team for{" "}
                    <strong>{patient?.display_name}</strong>?
                  </p>
                  <p style={{ marginTop: "8px", color: "#748882" }}>
                    They will lose access to view this patient&apos;s workspace and reminders.
                  </p>
                </>
              )}
            </div>

            {actionError && (
              <p className="form-error" role="alert" style={{ marginBottom: "12px" }}>
                {actionError}
              </p>
            )}

            <div className="modal-footer">
              <button
                type="button"
                className="button secondary"
                onClick={() => setConfirmDialog(null)}
                disabled={actionLoading}
              >
                Cancel
              </button>
              <button
                type="button"
                className={confirmDialog.type === "transfer" ? "button primary" : "danger-button"}
                onClick={handleConfirmAction}
                disabled={actionLoading}
              >
                {actionLoading
                  ? "Processing…"
                  : confirmDialog.type === "transfer"
                  ? "Confirm Transfer"
                  : "Confirm Removal"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}





