import { Link, useOutletContext } from "react-router-dom";
import { EmptyPatientsState, ErrorState, LoadingState, PageIntro, Reminders } from "../../components/caregiver/CaregiverWidgets";

export default function RemindersPage() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState message="Loading reminders…" />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients || !data.dashboard) return <EmptyPatientsState />;
  return <><PageIntro eyebrow="Patient support" title="Reminders" patient={data.dashboard.patient} action={<Link className="button secondary back-button" to={`/app/patients/${data.selectedId}`}>Back to overview</Link>} /><Reminders reminders={data.reminders} patientId={data.selectedId} onCreated={data.reload} /></>;
}

