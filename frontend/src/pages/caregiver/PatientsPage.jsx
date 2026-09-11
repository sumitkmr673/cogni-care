import { Link, useOutletContext } from "react-router-dom";
import { EmptyPatientsState, ErrorState, LoadingState, PageIntro, initials } from "../../components/caregiver/CaregiverWidgets";

export default function PatientsPage() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState message="Loading assigned patients…" />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  return <><PageIntro eyebrow="Caregiver portal" title="Patients" /><div className="patient-list-grid">{data.emptyPatients ? <EmptyPatientsState /> : data.patients.map((patient) => <Link className="patient-list-card" key={patient.id} to={`/app/patients/${patient.id}`}><div className="avatar patient large">{initials(patient.display_name)}</div><div><span className="eyebrow">Assigned patient</span><h2>{patient.display_name}</h2><p>{patient.preferred_language || "English"} · {patient.timezone || "Local time"}</p></div></Link>)}</div></>;
}
