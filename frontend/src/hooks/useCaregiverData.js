import { useCallback, useEffect, useState } from "react";
import {
  clearToken,
  createPatientReminder,
  getPatientDashboard,
  getPatientReminders,
  getPatientSessions,
  getPatientTrends,
  getPatients,
} from "../api";

export default function useCaregiverData(patientId) {
  const [patients, setPatients] = useState([]);
  const [selectedId, setSelectedId] = useState(patientId || "");
  const [dashboard, setDashboard] = useState(null);
  const [performance, setPerformance] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [reminders, setReminders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [emptyPatients, setEmptyPatients] = useState(false);

  const load = useCallback(async (requestedId = patientId) => {
    setLoading(true);
    setError("");
    try {
      const patientList = await getPatients();
      const nextId = requestedId || patientList.patients[0]?.id || "";
      setPatients(patientList.patients);
      setSelectedId(nextId);
      if (!nextId) {
        setDashboard(null);
        setPerformance([]);
        setSessions([]);
        setReminders([]);
        setEmptyPatients(true);
        return;
      }
      const [nextDashboard, nextTrends, nextSessions, nextReminders] = await Promise.all([
        getPatientDashboard(nextId),
        getPatientTrends(nextId),
        getPatientSessions(nextId, 50),
        getPatientReminders(nextId),
      ]);
      setEmptyPatients(false);
      setDashboard(nextDashboard);
      setPerformance(nextTrends.metrics);
      setSessions(nextSessions);
      setReminders(nextReminders);
    } catch (requestError) {
      if (requestError.status === 401) clearToken();
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [patientId]);

  useEffect(() => { load(patientId); }, [load, patientId]);

  const createReminder = useCallback(async (reminder) => {
    await createPatientReminder(selectedId, reminder);
    await load(selectedId);
  }, [load, selectedId]);

  return {
    patients,
    selectedId,
    dashboard,
    performance,
    sessions,
    reminders,
    loading,
    error,
    emptyPatients,
    reload: () => load(selectedId),
    loadPatient: load,
    createReminder,
  };
}
