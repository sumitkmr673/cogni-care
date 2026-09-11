import { useState } from "react";
import { Navigate, Outlet, Route, Routes, useNavigate } from "react-router-dom";
import { LoadingState } from "./components/caregiver/CaregiverWidgets";
import CaregiverLayout from "./components/layout/CaregiverLayout";
import useAuth from "./hooks/useAuth";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import ActivityPage from "./pages/caregiver/GameActivityPage";
import OverviewPage from "./pages/caregiver/OverviewPage";
import PatientDetailPage from "./pages/caregiver/PatientDetailPage";
import PatientsPage from "./pages/caregiver/PatientsPage";
import PerformancePage from "./pages/caregiver/PerformancePage";
import ProfilePage from "./pages/caregiver/ProfilePage";
import RemindersPage from "./pages/caregiver/RemindersPage";

function ProtectedRoutes({ authenticated, signedOut, caregiver, onSignOut }) {
  if (signedOut) return <Navigate to="/" replace />;
  if (!authenticated) return <Navigate to="/login" replace />;
  return <Outlet context={{ caregiver, onSignOut }} />;
}

export default function App() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [signedOut, setSignedOut] = useState(false);
  if (auth.status === "checking") return <LoadingState message="Checking your caregiver session…" />;
  const signOut = () => {
    setSignedOut(true);
    auth.signOut();
    navigate("/", { replace: true });
  };

  return (
    <Routes>
      <Route path="/" element={auth.status === "authenticated" ? <Navigate to="/app" replace /> : <HomePage />} />
      <Route path="/login" element={<LoginPage authenticated={auth.status === "authenticated"} onSignIn={auth.signIn} />} />
      <Route element={<ProtectedRoutes authenticated={auth.status === "authenticated"} signedOut={signedOut} caregiver={auth.caregiver} onSignOut={signOut} />}>
        <Route path="/app" element={<CaregiverLayout caregiver={auth.caregiver} onSignOut={signOut} />}>
          <Route index element={<OverviewPage />} />
          <Route path="patients" element={<PatientsPage />} />
          <Route path="patients/:patientId" element={<PatientDetailPage />} />
          <Route path="patients/:patientId/performance" element={<PerformancePage />} />
          <Route path="patients/:patientId/activity" element={<ActivityPage />} />
          <Route path="patients/:patientId/reminders" element={<RemindersPage />} />
          <Route path="profile" element={<ProfilePage caregiver={auth.caregiver} onSignOut={signOut} />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
