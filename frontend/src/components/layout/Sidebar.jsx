import { NavLink } from "react-router-dom";
import { clearToken } from "../../api";
import { Icon } from "../Icons";

const sections = [
  ["Overview", "/app", "grid"],
  ["Patients", "/app/patients", "users"],
  ["Performance", "/app/patients/current/performance", "trend"],
  ["Game Activity", "/app/patients/current/activity", "game"],
  ["Reminders", "/app/patients/current/reminders", "bell"],
];

export default function Sidebar({ patientId, caregiver, onSignOut }) {
  const resolvedSections = sections.map(([label, path, icon]) => [
    label,
    path.replace("/current", patientId ? `/${patientId}` : "/current"),
    icon,
  ]);

  function signOut() {
    clearToken();
    onSignOut();
  }

  return (
    <aside className="caregiver-sidebar">
      <div className="sidebar-brand"><div className="brand"><div className="brand-mark"><span /><span /><span /></div><div><strong>Cogni<span>-</span>Care</strong><small>Caregiver support</small></div></div></div>
      <nav className="sidebar-nav" aria-label="Caregiver navigation">
        <span className="sidebar-label">Caregiver portal</span>
        {resolvedSections.map(([label, path, icon]) => (
          <NavLink key={label} to={path} end={label === "Overview"} className={({ isActive }) => isActive ? "active" : ""}>
            <Icon name={icon} size={17} />{label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-account">
        <NavLink to="/app/profile" className={({ isActive }) => isActive ? "active" : ""}><Icon name="users" size={17} />Profile</NavLink>
        <button type="button" onClick={signOut}><Icon name="arrow" size={17} />Sign out</button>
        <div className="sidebar-caregiver"><div className="avatar doctor">{caregiver?.display_name?.split(/\s+/).map((part) => part[0]).slice(0, 2).join("") || "CC"}</div><span>{caregiver?.display_name || "Caregiver"}</span></div>
      </div>
    </aside>
  );
}
