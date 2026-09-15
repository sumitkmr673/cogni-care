import { NavLink, useLocation } from "react-router-dom";
import { clearToken } from "../../api";
import { Icon } from "../Icons";

export default function Sidebar({ patientId, caregiver, onSignOut }) {
  const location = useLocation();

  const sections = [
    { label: "Overview", path: patientId ? `/app/patients/${patientId}` : "/app/patients", icon: "grid", key: "overview" },
    { label: "Patients", path: "/app/patients", icon: "users", key: "patients" },
    { label: "Performance", path: patientId ? `/app/patients/${patientId}/performance` : "/app/patients", icon: "trend", key: "performance" },
    { label: "Game Activity", path: patientId ? `/app/patients/${patientId}/activity` : "/app/patients", icon: "game", key: "activity" },
    { label: "Reminders", path: patientId ? `/app/patients/${patientId}/reminders` : "/app/patients", icon: "bell", key: "reminders" },
    { label: "Care Team", path: patientId ? `/app/patients/${patientId}/care-team` : "/app/patients", icon: "users", key: "care-team" },
  ];

  function signOut() {
    clearToken();
    onSignOut();
  }

  return (
    <aside className="caregiver-sidebar">
      <div className="sidebar-brand"><div className="brand"><div className="brand-mark"><span /><span /><span /></div><div><strong>Cogni<span>-</span>Care</strong><small>Caregiver support</small></div></div></div>
      <nav className="sidebar-nav" aria-label="Caregiver navigation">
        <span className="sidebar-label">Caregiver portal</span>
        {sections.map(({ label, path, icon, key }) => (
          <NavLink key={label} to={path} className={() => {
            const currentPath = location.pathname;
            const isActive = key === "overview"
              ? currentPath === "/app" || (patientId && currentPath === `/app/patients/${patientId}`)
              : key === "patients"
                ? currentPath === "/app/patients"
                : currentPath.endsWith(`/${key}`);
            return isActive ? "active" : "";
          }}>
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
