import { Navigate, useOutletContext } from "react-router-dom";
import { ErrorState, LoadingState, PerformanceContent } from "../../components/caregiver/CaregiverWidgets";

export default function PerformancePage() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState />;
  if (data.error) return <ErrorState message={data.error} onRetry={data.reload} />;
  if (data.emptyPatients || !data.dashboard) return <Navigate to="/app/patients" replace />;
  return <PerformanceContent data={data} />;
}
