import { Navigate, Outlet, useLocation, useParams } from "react-router-dom";
import { hasToken } from "../../api";
import Sidebar from "./Sidebar";
import useCaregiverData from "../../hooks/useCaregiverData";

export default function CaregiverLayout({ caregiver, onSignOut }) {
  const location = useLocation();
  const params = useParams();
  const data = useCaregiverData(params.patientId && params.patientId !== "current" ? params.patientId : undefined);

  if (!hasToken() || data.error === "Not authenticated" || data.error === "Could not validate credentials") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return (
    <div className="caregiver-app">
      <Sidebar patientId={data.selectedId} caregiver={caregiver} onSignOut={onSignOut} />
      <div className="caregiver-main">
        <header className="caregiver-topbar">
          <div><span className="eyebrow">Caregiver workspace</span><strong>{data.dashboard?.patient?.display_name ? `Patient: ${data.dashboard.patient.display_name}` : "Your care overview"}</strong></div>
          <span className="connection-status"><i className="online-dot" />Connected</span>
        </header>
        <main className="caregiver-page">
          <Outlet context={data} />
        </main>
        <footer className="app-footer"><span><i className="online-dot" />Connected to Cogni-Care backend</span><span>Cogni-Care · Cognitive game performance support</span></footer>
      </div>
    </div>
  );
}
