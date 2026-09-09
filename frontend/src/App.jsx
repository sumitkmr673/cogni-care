import { useEffect, useMemo, useState } from "react";
import { getPatientDashboard, getPatientPerformance, getPatients } from "./api";
import { Icon } from "./components/Icons";
import PerformanceChart, { formatShortDate } from "./components/PerformanceChart";
import StatCard from "./components/StatCard";

function formatDate(value, options = { day: "numeric", month: "short" }) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("en-IN", options).format(new Date(value));
}

function LoadingState() {
  return <div className="state-card"><div className="spinner" /><p>Loading patient data…</p></div>;
}

function ErrorState({ message, onRetry }) {
  return <div className="state-card error-state"><strong>We couldn’t load the dashboard</strong><p>{message}</p><button className="button secondary" onClick={onRetry}>Try again</button></div>;
}

function Topbar({ screen, setScreen, patient, onRefresh }) {
  return (
    <header className="topbar">
      <div className="brand">
        <div className="brand-mark"><span /><span /><span /></div>
        <div><strong>Cogni<span>-</span>Care</strong><small>Caregiver support</small></div>
      </div>
      <nav className="main-nav" aria-label="Primary navigation">
        <button className={screen === "dashboard" ? "active" : ""} onClick={() => setScreen("dashboard")}><Icon name="grid" size={17} />Overview</button>
        <button className={screen === "performance" ? "active" : ""} onClick={() => setScreen("performance")}><Icon name="trend" size={17} />Performance</button>
      </nav>
      <div className="top-actions">
        <button className="icon-button" aria-label="Refresh data" onClick={onRefresh}><Icon name="bell" size={19} /><i /></button>
        <div className="caregiver-chip"><div className="avatar doctor">AM</div><div><strong>Dr. Ananya Mehta</strong><small>Caregiver · Demo</small></div><Icon name="chevron" size={15} /></div>
      </div>
    </header>
  );
}

function PageIntro({ title, eyebrow, patient, action }) {
  return (
    <div className="page-intro">
      <div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>Clear, compassionate insights from cognitive game activity.</p></div>
      {patient && <div className="patient-chip"><div className="avatar patient">MS</div><div><small>Patient</small><strong>{patient.display_name}</strong></div>{action}</div>}
    </div>
  );
}

function PatientSelector({ patients, selectedId, onChange }) {
  return <label className="patient-selector"><span>Viewing patient</span><select value={selectedId || ""} onChange={(event) => onChange(event.target.value)}>{patients.map((patient) => <option key={patient.id} value={patient.id}>{patient.display_name}</option>)}</select><Icon name="chevron" size={16} /></label>;
}

function SessionsTable({ sessions, compact = false }) {
  return (
    <div className={`session-list ${compact ? "compact" : ""}`}>
      <div className="list-heading"><span>Recent activity</span><span>{sessions.length} sessions</span></div>
      {sessions.map((session) => (
        <div className="session-row" key={session.id}>
          <div className={`game-badge ${session.game_code.toLowerCase()}`}><Icon name="game" size={17} /></div>
          <div className="session-main"><strong>{session.game_name}</strong><span>{formatDate(session.started_at, { day: "numeric", month: "short", year: "numeric" })} · Level {session.difficulty_level}</span></div>
          {session.result ? <div className="session-score"><strong>{Number(session.result.accuracy).toFixed(0)}%</strong><span>accuracy</span></div> : <span className="status-pill">{session.status}</span>}
          {!compact && <div className="session-duration"><Icon name="clock" size={14} />{session.result?.response_time_ms ? `${(session.result.response_time_ms / 1000).toFixed(1)}s` : "—"}</div>}
        </div>
      ))}
    </div>
  );
}

function Reminders({ reminders }) {
  return (
    <section className="panel reminders-panel">
      <div className="panel-heading"><div><span className="eyebrow">Stay on track</span><h2>Upcoming reminders</h2></div><Icon name="bell" size={19} /></div>
      {reminders.length ? reminders.map((reminder) => (
        <div className="reminder-row" key={reminder.id}><div className={`reminder-icon ${reminder.reminder_type.toLowerCase()}`}><Icon name={reminder.reminder_type === "GAME" ? "game" : reminder.reminder_type === "APPOINTMENT" ? "calendar" : "users"} size={17} /></div><div><strong>{reminder.title}</strong><span>{formatDate(reminder.scheduled_at, { weekday: "short", day: "numeric", month: "short" })} · {formatDate(reminder.scheduled_at, { hour: "numeric", minute: "2-digit" })}</span></div></div>
      )) : <p className="muted">No upcoming reminders.</p>}
    </section>
  );
}

function Dashboard({ dashboard, performance, patients, selectedId, onPatientChange, onOpenPerformance }) {
  const latest = dashboard.latest_performance;
  const sessions = dashboard.recent_sessions;
  const trendMetrics = performance.slice(-7);
  return (
    <>
      <PageIntro eyebrow="Caregiver overview" title="Good morning, Ananya" patient={dashboard.patient} action={<PatientSelector patients={patients} selectedId={selectedId} onChange={onPatientChange} />} />
      <div className="dashboard-grid">
        <section className="welcome-card"><div><span className="eyebrow">Patient snapshot</span><h2>{dashboard.patient.display_name}</h2><p>English · {dashboard.patient.timezone || "Local time"} · Game activity overview</p></div><div className="welcome-avatar">MS</div><div className="welcome-foot"><span><i className="online-dot" />Activity synced today</span><button className="text-button" onClick={onOpenPerformance}>View full performance <Icon name="arrow" size={15} /></button></div></section>
        <div className="stats-grid"><StatCard icon="game" label="Games completed" value={latest?.games_completed ?? "—"} suffix=" today" tone="mint" /><StatCard icon="trend" label="Average accuracy" value={latest?.average_accuracy ? Number(latest.average_accuracy).toFixed(0) : "—"} suffix="%" tone="blue" /><StatCard icon="users" label="Memory score" value={latest?.memory_score ? Number(latest.memory_score).toFixed(0) : "—"} suffix="/100" tone="peach" /><StatCard icon="trend" label="Attention score" value={latest?.attention_score ? Number(latest.attention_score).toFixed(0) : "—"} suffix="/100" tone="lavender" /></div>
        <section className="panel chart-panel"><div className="panel-heading"><div><span className="eyebrow">Cognitive game performance</span><h2>Performance trend</h2></div><button className="text-button" onClick={onOpenPerformance}>Details <Icon name="arrow" size={15} /></button></div><PerformanceChart metrics={trendMetrics} /></section>
        <Reminders reminders={dashboard.active_reminders} />
        <section className="panel sessions-panel"><div className="panel-heading"><div><span className="eyebrow">Recent activity</span><h2>Latest game sessions</h2></div><button className="text-button" onClick={onOpenPerformance}>See all <Icon name="arrow" size={15} /></button></div><SessionsTable sessions={sessions.slice(0, 5)} compact /></section>
      </div>
    </>
  );
}

function Performance({ dashboard, performance, onBack }) {
  const sessions = dashboard.recent_sessions;
  const games = useMemo(() => Object.values(sessions.reduce((result, session) => { if (!result[session.game_code]) result[session.game_code] = { ...session, attempts: 0, accuracy: 0 }; result[session.game_code].attempts += 1; result[session.game_code].accuracy += Number(session.result?.accuracy || 0); return result; }, {})).map((game) => ({ ...game, accuracy: game.accuracy / game.attempts })), [sessions]);
  const latest = dashboard.latest_performance;
  return (
    <>
      <PageIntro eyebrow="Patient performance" title="Cognitive game performance" patient={dashboard.patient} action={<button className="button secondary back-button" onClick={onBack}>Back to overview</button>} />
      <div className="performance-grid"><div className="performance-summary"><div className="profile-large"><div className="avatar patient large">MS</div><div><strong>{dashboard.patient.display_name}</strong><span>Patient profile · English</span><small>Performance indicators are based on game activity, not a medical diagnosis.</small></div></div><div className="metric-row"><div><span>Memory performance</span><strong>{latest?.memory_score ? Number(latest.memory_score).toFixed(0) : "—"}<small>/100</small></strong></div><div><span>Attention performance</span><strong>{latest?.attention_score ? Number(latest.attention_score).toFixed(0) : "—"}<small>/100</small></strong></div><div><span>Game accuracy</span><strong>{latest?.average_accuracy ? Number(latest.average_accuracy).toFixed(0) : "—"}<small>%</small></strong></div><div><span>Avg. response time</span><strong>{latest?.average_response_time_ms ? (latest.average_response_time_ms / 1000).toFixed(1) : "—"}<small>s</small></strong></div></div></div><section className="panel full-chart-panel"><div className="panel-heading"><div><span className="eyebrow">Daily history</span><h2>Performance trend</h2></div><span className="date-range">{formatShortDate(performance[0]?.metric_date)} — {formatShortDate(performance.at(-1)?.metric_date)}</span></div><PerformanceChart metrics={performance} /></section><section className="panel game-breakdown"><div className="panel-heading"><div><span className="eyebrow">Across all activities</span><h2>Game-by-game performance</h2></div></div><div className="game-table">{games.map((game) => <div className="game-table-row" key={game.game_code}><div className={`game-badge ${game.game_code.toLowerCase()}`}><Icon name="game" size={17} /></div><div className="game-table-name"><strong>{game.game_name}</strong><span>{game.attempts} completed · Latest level {game.difficulty_level}</span></div><div className="bar-track"><i style={{ width: `${Math.min(game.accuracy, 100)}%` }} /></div><strong className="game-percent">{game.accuracy.toFixed(0)}%</strong></div>)}</div></section><section className="panel performance-sessions"><div className="panel-heading"><div><span className="eyebrow">Detailed history</span><h2>Recent sessions & results</h2></div></div><SessionsTable sessions={sessions} /></section></div>
    </>
  );
}

export default function App() {
  const [screen, setScreen] = useState("dashboard");
  const [patients, setPatients] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const [dashboard, setDashboard] = useState(null);
  const [performance, setPerformance] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  async function load(patientId) {
    setLoading(true);
    setError(null);
    try {
      const patientList = await getPatients();
      const nextId = patientId || patientList.patients[0]?.id;
      if (!nextId) throw new Error("No demo patients are available.");
      const [nextDashboard, nextPerformance] = await Promise.all([getPatientDashboard(nextId), getPatientPerformance(nextId)]);
      setPatients(patientList.patients);
      setSelectedId(nextId);
      setDashboard(nextDashboard);
      setPerformance(nextPerformance.metrics);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  return (
    <div className="app-shell">
      <Topbar screen={screen} setScreen={setScreen} patient={dashboard?.patient} onRefresh={() => load(selectedId)} />
      <main className="page">
        {loading && <LoadingState />}
        {!loading && error && <ErrorState message={error} onRetry={() => load(selectedId)} />}
        {!loading && !error && dashboard && <>{screen === "dashboard" ? <Dashboard dashboard={dashboard} performance={performance} patients={patients} selectedId={selectedId} onPatientChange={(id) => load(id)} onOpenPerformance={() => setScreen("performance")} /> : <Performance dashboard={dashboard} performance={performance} onBack={() => setScreen("dashboard")} />}</>}
      </main>
      <footer className="app-footer"><span><i className="online-dot" />Demo environment</span><span>Cogni-Care · Cognitive game performance support</span></footer>
    </div>
  );
}
