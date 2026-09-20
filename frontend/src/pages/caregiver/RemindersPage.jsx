import { useOutletContext } from "react-router-dom";
import {
  EmptyPatientsState,
  ErrorState,
  LoadingState,
  RemindersContent,
} from "../../components/caregiver/CaregiverWidgets";

export default function RemindersPage() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState message="Loading reminders…" />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients || !data.dashboard) return <EmptyPatientsState />;

  return <RemindersContent data={data} />;
}


