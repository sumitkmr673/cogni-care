import { useOutletContext } from "react-router-dom";
import {
  CareTeamContent,
  EmptyPatientsState,
  ErrorState,
  LoadingState,
} from "../../components/caregiver/CaregiverWidgets";

export default function CareTeamPage() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState message="Loading care team…" />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients) return <EmptyPatientsState />;

  return <CareTeamContent data={data} />;
}
