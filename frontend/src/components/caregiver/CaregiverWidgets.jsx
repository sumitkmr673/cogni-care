import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createPatientReminder } from "../../api";
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

export function LoadingState({ message = "Loading patient data…" }) {
  return <div className="state-card"><div className="spinner" /><p>{message}</p></div>;
}

export function ErrorState({ message, onRetry }) {
  return <div className="state-card error-state"><strong>We couldn’t load the caregiver workspace</strong><p>{message}</p><button className="button secondary" onClick={onRetry}>Try again</button></div>;
}

export function EmptyPatientsState() {
  return <div className="state-card"><strong>No assigned patients yet</strong><p>Your caregiver account does not have any patient assignments.</p></div>;
}

export function PageIntro({ title, eyebrow, patient, action }) {
  return <div className="page-intro"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>Clear, compassionate insights from cognitive game activity.</p></div>{patient && <div className="patient-chip"><div className="avatar patient">{initials(patient.display_name)}</div><div><small>Patient</small><strong>{patient.display_name}</strong></div>{action}</div>}</div>;
}

export function PatientSelector({ patients, selectedId }) {
  const navigate = useNavigate();
  return <label className="patient-selector"><span>Viewing patient</span><select value={selectedId || ""} onChange={(event) => navigate(`/app/patients/${event.target.value}`)}>{patients.map((patient) => <option key={patient.id} value={patient.id}>{patient.display_name}</option>)}</select><Icon name="chevron" size={16} /></label>;
}

export function SessionsTable({ sessions, compact = false }) {
  return <div className={`session-list ${compact ? "compact" : ""}`}><div className="list-heading"><span>Recent activity</span><span>{sessions.length} sessions</span></div>{sessions.length ? sessions.map((session) => <div className="session-row" key={session.id}><div className={`game-badge ${session.game_code.toLowerCase()}`}><Icon name="game" size={17} /></div><div className="session-main"><strong>{session.game_name}</strong><span>{formatDate(session.started_at, { day: "numeric", month: "short", year: "numeric" })} · Level {session.difficulty_level}{session.result?.score != null ? ` · Score ${session.result.score}` : ""}{session.result?.correct_answers != null && session.result?.total_questions != null ? ` · ${session.result.correct_answers}/${session.result.total_questions} correct` : ""}</span></div>{session.result ? <div className="session-score"><strong>{session.result.accuracy == null ? "—" : `${Number(session.result.accuracy).toFixed(0)}%`}</strong><span>accuracy</span></div> : <span className="status-pill">{session.status}</span>}{!compact && <div className="session-duration"><Icon name="clock" size={14} />{session.result?.response_time_ms ? `${(session.result.response_time_ms / 1000).toFixed(1)}s` : "—"}</div>}</div>) : <p className="muted">No game sessions yet.</p>}</div>;
}

export function Reminders({ reminders, patientId, onCreated }) {
  const [form, setForm] = useState({ title: "", reminder_type: "GAME", scheduled_at: "", is_recurring: false, recurrence_rule: "" });
  const [state, setState] = useState({ loading: false, error: "", success: "" });
  async function submit(event) {
    event.preventDefault();
    setState({ loading: true, error: "", success: "" });
    try {
      await createPatientReminder(patientId, { ...form, scheduled_at: new Date(form.scheduled_at).toISOString(), recurrence_rule: form.is_recurring ? form.recurrence_rule.trim() : null });
      setForm({ title: "", reminder_type: "GAME", scheduled_at: "", is_recurring: false, recurrence_rule: "" });
      setState({ loading: false, error: "", success: "Reminder created." });
      onCreated();
    } catch (error) {
      setState({ loading: false, error: error.message, success: "" });
    }
  }
  return <section className="panel reminders-panel"><div className="panel-heading"><div><span className="eyebrow">Stay on track</span><h2>Upcoming reminders</h2></div><Icon name="bell" size={19} /></div>{reminders.length ? reminders.map((reminder) => <div className="reminder-row" key={reminder.id}><div className={`reminder-icon ${reminder.reminder_type.toLowerCase()}`}><Icon name={reminder.reminder_type === "GAME" ? "game" : "calendar"} size={17} /></div><div><strong>{reminder.title}</strong><span>{formatDate(reminder.scheduled_at, { weekday: "short", day: "numeric", month: "short" })} · {formatDate(reminder.scheduled_at, { hour: "numeric", minute: "2-digit" })}</span></div></div>) : <p className="muted">No upcoming reminders.</p>}<form className="reminder-form" onSubmit={submit}><div className="form-row"><input aria-label="Reminder title" placeholder="New reminder title" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} required /><select aria-label="Reminder type" value={form.reminder_type} onChange={(event) => setForm({ ...form, reminder_type: event.target.value })}><option>GAME</option><option>ACTIVITY</option><option>APPOINTMENT</option><option>MEDICATION</option><option>OTHER</option></select></div><div className="form-row"><input aria-label="Reminder date and time" type="datetime-local" value={form.scheduled_at} onChange={(event) => setForm({ ...form, scheduled_at: event.target.value })} required /><button className="button secondary" type="submit" disabled={state.loading}>{state.loading ? "Saving…" : "Add reminder"}</button></div>{state.error && <p className="form-error">{state.error}</p>}{state.success && <p className="form-success">{state.success}</p>}</form></section>;
}

export function OverviewContent({ data }) {
  const { dashboard, performance, sessions, reminders, selectedId, patients, reload } = data;
  const latest = dashboard.latest_performance;
  return <><PageIntro eyebrow="Caregiver overview" title={`Good morning, ${dashboard.caregiver_relationship?.display_name?.split(" ")[1] || "Caregiver"}`} patient={dashboard.patient} action={<PatientSelector patients={patients} selectedId={selectedId} />} /><div className="dashboard-grid"><section className="welcome-card"><div><span className="eyebrow">Patient snapshot</span><h2>{dashboard.patient.display_name}</h2><p>{dashboard.patient.preferred_language || "English"} · {dashboard.patient.timezone || "Local time"} · Game activity overview</p></div><div className="welcome-avatar">{initials(dashboard.patient.display_name)}</div><div className="welcome-foot"><span><i className="online-dot" />Activity synced from backend</span><Link className="text-button" to={`/app/patients/${selectedId}/performance`}>View full performance <Icon name="arrow" size={15} /></Link></div></section><div className="stats-grid"><StatCard icon="game" label="Games completed" value={latest?.games_completed ?? "—"} suffix=" today" tone="mint" /><StatCard icon="trend" label="Average accuracy" value={latest?.average_accuracy == null ? "—" : Number(latest.average_accuracy).toFixed(0)} suffix="%" tone="blue" /><StatCard icon="users" label="Memory score" value={latest?.memory_score == null ? "—" : Number(latest.memory_score).toFixed(0)} suffix="/100" tone="peach" /><StatCard icon="trend" label="Attention score" value={latest?.attention_score == null ? "—" : Number(latest.attention_score).toFixed(0)} suffix="/100" tone="lavender" /></div><section className="panel chart-panel"><div className="panel-heading"><div><span className="eyebrow">Cognitive game performance</span><h2>Performance trend</h2></div><Link className="text-button" to={`/app/patients/${selectedId}/performance`}>Details <Icon name="arrow" size={15} /></Link></div><PerformanceChart metrics={performance.slice(-7)} /></section><Reminders reminders={reminders} patientId={selectedId} onCreated={reload} /><section className="panel sessions-panel"><div className="panel-heading"><div><span className="eyebrow">Recent activity</span><h2>Latest game sessions</h2></div><Link className="text-button" to={`/app/patients/${selectedId}/activity`}>See all <Icon name="arrow" size={15} /></Link></div><SessionsTable sessions={sessions.slice(0, 5)} compact /></section></div></>;
}

export function PerformanceContent({ data }) {
  const { dashboard, performance, sessions } = data;
  const games = useMemo(() => Object.values(sessions.reduce((result, session) => { if (!result[session.game_code]) result[session.game_code] = { ...session, attempts: 0, accuracy: 0 }; result[session.game_code].attempts += 1; result[session.game_code].accuracy += Number(session.result?.accuracy || 0); return result; }, {})).map((game) => ({ ...game, accuracy: game.accuracy / game.attempts })), [sessions]);
  const latest = dashboard.latest_performance;
  return <><PageIntro eyebrow="Patient performance" title="Cognitive game performance" patient={dashboard.patient} action={<Link className="button secondary back-button" to="/app">Back to overview</Link>} /><div className="performance-grid"><div className="performance-summary"><div className="profile-large"><div className="avatar patient large">{initials(dashboard.patient.display_name)}</div><div><strong>{dashboard.patient.display_name}</strong><span>Patient profile · {dashboard.patient.preferred_language || "English"}</span><small>Performance indicators are based on game activity, not a medical diagnosis.</small></div></div><div className="metric-row"><div><span>Memory performance</span><strong>{latest?.memory_score == null ? "—" : Number(latest.memory_score).toFixed(0)}<small>/100</small></strong></div><div><span>Attention performance</span><strong>{latest?.attention_score == null ? "—" : Number(latest.attention_score).toFixed(0)}<small>/100</small></strong></div><div><span>Game accuracy</span><strong>{latest?.average_accuracy == null ? "—" : Number(latest.average_accuracy).toFixed(0)}<small>%</small></strong></div><div><span>Avg. response time</span><strong>{latest?.average_response_time_ms == null ? "—" : (latest.average_response_time_ms / 1000).toFixed(1)}<small>s</small></strong></div></div></div><section className="panel full-chart-panel"><div className="panel-heading"><div><span className="eyebrow">Daily history</span><h2>Performance trend</h2></div></div><PerformanceChart metrics={performance} /></section><section className="panel game-breakdown"><div className="panel-heading"><div><span className="eyebrow">Across all activities</span><h2>Game-by-game performance</h2></div></div><div className="game-table">{games.length ? games.map((game) => <div className="game-table-row" key={game.game_code}><div className={`game-badge ${game.game_code.toLowerCase()}`}><Icon name="game" size={17} /></div><div className="game-table-name"><strong>{game.game_name}</strong><span>{game.attempts} completed · Latest level {game.difficulty_level}</span></div><div className="bar-track"><i style={{ width: `${Math.min(game.accuracy, 100)}%` }} /></div><strong className="game-percent">{game.accuracy.toFixed(0)}%</strong></div>) : <p className="muted">No game performance yet.</p>}</div></section><section className="panel performance-sessions"><div className="panel-heading"><div><span className="eyebrow">Detailed history</span><h2>Recent sessions & results</h2></div></div><SessionsTable sessions={sessions} /></section></div></>;
}
