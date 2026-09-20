import { useOutletContext } from "react-router-dom";
import {
  ActivityContent,
  EmptyPatientsState,
  ErrorState,
  LoadingState,
} from "../../components/caregiver/CaregiverWidgets";

export default function GameActivityPage() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState message="Loading game activity…" />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients || !data.dashboard) return <EmptyPatientsState />;
  return <ActivityContent data={data} />;
}


