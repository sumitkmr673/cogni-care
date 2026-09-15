import { Navigate, Outlet, useLocation, useParams } from "react-router-dom";
import { hasToken } from "../../api";
import Sidebar from "./Sidebar";
import useCaregiverData from "../../hooks/useCaregiverData";
import { PatientSelector, initials } from "../caregiver/CaregiverWidgets";

export default function CaregiverLayout({ caregiver, onSignOut }) {
  const location = useLocation();
  const params = useParams();
  const data = useCaregiverData(params.patientId);
  const pageTitle = location.pathname === "/app/profile"
    ? "Profile"
    : location.pathname.endsWith("/performance")
      ? "Performance"
      : location.pathname.endsWith("/activity")
        ? "Game activity"
        : location.pathname.endsWith("/reminders")
          ? "Reminders"
          : location.pathname.endsWith("/care-team")
            ? "Care team"
            : location.pathname === "/app/patients"
              ? "Patients directory"
              : "Patient overview";

  if (!hasToken() || data.error === "Not authenticated" || data.error === "Could not validate credentials") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return (
    <div className="caregiver-app">
      <Sidebar patientId={data.selectedId} caregiver={caregiver} onSignOut={onSignOut} />
      <div className="caregiver-main">
        <header className="caregiver-topbar">
          <div><span className="eyebrow">Caregiver workspace</span><strong>{pageTitle}</strong></div>
          <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
            {data.patients?.length > 0 && data.selectedId && (
              <div
                className="patient-chip"
                style={{
                  minWidth: "unset",
                  padding: "5px 10px 5px 8px",
                  border: "1px solid #dbe7e1",
                  borderRadius: "12px",
                }}
              >
                <div
                  className="avatar patient"
                  style={{ width: "26px", height: "26px", fontSize: "10px" }}
                >
                  {initials(data.dashboard?.patient?.display_name || "")}
                </div>
                <PatientSelector patients={data.patients} selectedId={data.selectedId} />
              </div>
            )}
            <span className="connection-status"><i className="online-dot" />Connected</span>
          </div>
        </header>
        <main className="caregiver-page">
          <Outlet context={data} />
        </main>
        <footer className="app-footer"><span><i className="online-dot" />Connected to Cogni-Care backend</span><span>Cogni-Care · Cognitive game performance support</span></footer>
      </div>
    </div>
  );
}
