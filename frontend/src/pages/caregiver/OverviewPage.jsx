import { Navigate, useOutletContext } from "react-router-dom";
import { EmptyPatientsState, ErrorState, LoadingState, OverviewContent } from "../../components/caregiver/CaregiverWidgets";

export default function OverviewPage() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients) return <EmptyPatientsState />;
  if (!data.dashboard) return <Navigate to="/app/patients" replace />;
  return <OverviewContent data={data} />;
}
