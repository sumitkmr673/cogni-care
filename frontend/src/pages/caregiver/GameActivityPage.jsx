import { Link, useOutletContext } from "react-router-dom";
import { EmptyPatientsState, ErrorState, LoadingState, PageIntro, SessionsTable } from "../../components/caregiver/CaregiverWidgets";

export default function GameActivityPage() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState message="Loading game activity…" />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients || !data.dashboard) return <EmptyPatientsState />;
  return <><PageIntro eyebrow="Patient activity" title="Game activity" patient={data.dashboard.patient} action={<Link className="button secondary back-button" to={`/app/patients/${data.selectedId}`}>Back to overview</Link>} /><section className="panel activity-page-panel"><div className="panel-heading"><div><span className="eyebrow">Recorded sessions</span><h2>Recent cognitive game activity</h2></div></div><SessionsTable sessions={data.sessions} /></section></>;
}

