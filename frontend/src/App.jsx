import { Navigate, Outlet, Route, Routes, useNavigate, useOutletContext } from "react-router-dom";
import ErrorBoundary from "./components/ErrorBoundary";
import { LoadingState } from "./components/caregiver/CaregiverWidgets";
import CaregiverLayout from "./components/layout/CaregiverLayout";
import useAuth from "./hooks/useAuth";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ActivityPage from "./pages/caregiver/GameActivityPage";
import CareTeamPage from "./pages/caregiver/CareTeamPage";
import OverviewPage from "./pages/caregiver/OverviewPage";
import PatientsPage from "./pages/caregiver/PatientsPage";
import PerformancePage from "./pages/caregiver/PerformancePage";
import ProfilePage from "./pages/caregiver/ProfilePage";
import RemindersPage from "./pages/caregiver/RemindersPage";

function ProtectedRoutes({ authenticated, caregiver, onSignOut }) {
  if (!authenticated) return <Navigate to="/login" replace />;
  return (
    <ErrorBoundary>
      <Outlet context={{ caregiver, onSignOut }} />
    </ErrorBoundary>
  );
}

function AppIndex() {
  const data = useOutletContext();
  if (data.loading) return <LoadingState message="Loading your workspace…" />;
  if (data.emptyPatients || !data.selectedId) {
    return <Navigate to="/app/patients" replace />;
  }
  return <Navigate to={`/app/patients/${data.selectedId}`} replace />;
}

export default function App() {
  const auth = useAuth();
  const navigate = useNavigate();
  if (auth.status === "checking") return <LoadingState message="Checking your caregiver session…" />;
  const signOut = () => {
    auth.signOut();
    navigate("/", { replace: true });
  };

  return (
    <Routes>
      <Route path="/" element={auth.status === "authenticated" ? <Navigate to="/app" replace /> : <HomePage />} />
      <Route path="/login" element={<LoginPage authenticated={auth.status === "authenticated"} onSignIn={auth.signIn} />} />
      <Route path="/register" element={<RegisterPage authenticated={auth.status === "authenticated"} />} />
      <Route element={<ProtectedRoutes authenticated={auth.status === "authenticated"} caregiver={auth.caregiver} onSignOut={signOut} />}>
        <Route path="/app" element={<CaregiverLayout caregiver={auth.caregiver} onSignOut={signOut} />}>
          <Route index element={<AppIndex />} />
          <Route path="patients" element={<PatientsPage />} />
          <Route path="patients/:patientId" element={<OverviewPage />} />
          <Route path="patients/:patientId/performance" element={<PerformancePage />} />
          <Route path="patients/:patientId/activity" element={<ActivityPage />} />
          <Route path="patients/:patientId/reminders" element={<RemindersPage />} />
          <Route path="patients/:patientId/care-team" element={<CareTeamPage />} />
          <Route path="profile" element={<ProfilePage caregiver={auth.caregiver} onSignOut={signOut} />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
