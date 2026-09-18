import { useCallback, useEffect, useState } from "react";
import {
  addCareTeamMember as apiAddCareTeamMember,
  clearToken,
  createPatient as apiCreatePatient,
  createPatientReminder,
  getCareTeam,
  getPatientDashboard,
  getPatientReminders,
  getPatientSessions,
  getPatientTrends,
  getPatients,
  linkPatient as apiLinkPatient,
  removeCareTeamMember as apiRemoveCareTeamMember,
  transferPrimaryCaregiver as apiTransferPrimaryCaregiver,
} from "../api";

export default function useCaregiverData(patientId) {
  const [patients, setPatients] = useState([]);
  const [selectedId, setSelectedId] = useState(patientId || "");
  const [dashboard, setDashboard] = useState(null);
  const [performance, setPerformance] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [reminders, setReminders] = useState([]);
  const [careTeam, setCareTeam] = useState([]);
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
        setCareTeam([]);
        setEmptyPatients(true);
        return;
      }
      const [nextDashboard, nextTrends, nextSessions, nextReminders, nextCareTeam] = await Promise.all([
        getPatientDashboard(nextId),
        getPatientTrends(nextId),
        getPatientSessions(nextId, 50),
        getPatientReminders(nextId),
        getCareTeam(nextId).then((res) => res?.members || []).catch(() => []),
      ]);
      setEmptyPatients(false);
      setDashboard(nextDashboard);
      setPerformance(nextTrends.metrics);
      setSessions(nextSessions);
      setReminders(nextReminders);
      setCareTeam(nextCareTeam);
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

  const createPatient = useCallback(async (payload) => {
    const newPatient = await apiCreatePatient(payload);
    await load(newPatient.id);
    return newPatient;
  }, [load]);

  const linkPatient = useCallback(async (publicId) => {
    const linkedPatient = await apiLinkPatient(publicId);
    await load(linkedPatient.id);
    return linkedPatient;
  }, [load]);

  const addCareTeamMember = useCallback(async (caregiverPublicId) => {
    const member = await apiAddCareTeamMember(selectedId, caregiverPublicId);
    const updatedTeam = await getCareTeam(selectedId).then((res) => res?.members || []).catch(() => []);
    setCareTeam(updatedTeam);
    return member;
  }, [selectedId]);

  const transferPrimary = useCallback(async (caregiverId) => {
    const member = await apiTransferPrimaryCaregiver(selectedId, caregiverId);
    await load(selectedId);
    return member;
  }, [load, selectedId]);

  const removeCareTeamMember = useCallback(async (caregiverId) => {
    await apiRemoveCareTeamMember(selectedId, caregiverId);
    const updatedTeam = await getCareTeam(selectedId).then((res) => res?.members || []).catch(() => []);
    setCareTeam(updatedTeam);
  }, [selectedId]);

  return {
    patients,
    selectedId,
    dashboard,
    performance,
    sessions,
    reminders,
    careTeam,
    loading,
    error,
    emptyPatients,
    reload: () => load(selectedId),
    loadPatient: load,
    createReminder,
    createPatient,
    linkPatient,
    addCareTeamMember,
    transferPrimary,
    removeCareTeamMember,
  };
}

