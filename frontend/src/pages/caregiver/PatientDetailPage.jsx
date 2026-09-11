import { Link, useOutletContext } from "react-router-dom";
import { EmptyPatientsState, ErrorState, LoadingState, PageIntro, initials } from "../../components/caregiver/CaregiverWidgets";

export default function PatientDetailPage() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients || !data.dashboard) return <EmptyPatientsState />;
  const patient = data.dashboard.patient;
  const base = `/app/patients/${data.selectedId}`;
  return <><PageIntro eyebrow="Patient overview" title={patient.display_name} patient={patient} /><section className="patient-detail-card"><div className="avatar patient large">{initials(patient.display_name)}</div><div><span className="eyebrow">Assigned patient</span><h2>{patient.display_name}</h2><p>{patient.preferred_language || "English"} · {patient.timezone || "Local time"}</p></div></section><div className="detail-link-grid"><Link to={`${base}/performance`}><strong>Performance</strong><span>Review memory, attention, accuracy, and trends →</span></Link><Link to={`${base}/activity`}><strong>Game Activity</strong><span>View recorded cognitive game sessions →</span></Link><Link to={`${base}/reminders`}><strong>Reminders</strong><span>Manage upcoming reminders →</span></Link></div></>;
}
