const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";
export const DEMO_CAREGIVER_ID =
  import.meta.env.VITE_DEMO_CAREGIVER_ID ||
  "d0000000-0000-0000-0000-000000000001";

async function request(path) {
  const response = await fetch(`${API_BASE_URL}${path}`);
  if (!response.ok) {
    let detail = `Request failed with status ${response.status}`;
    try {
      const body = await response.json();
      detail = body.detail || detail;
    } catch {
      // Keep the HTTP error when the backend does not return JSON.
    }
    throw new Error(detail);
  }
  return response.json();
}

export function getPatients() {
  return request(`/patients?demo_caregiver_id=${DEMO_CAREGIVER_ID}`);
}

export function getPatientDashboard(patientId) {
  return request(
    `/patients/${patientId}/dashboard?demo_caregiver_id=${DEMO_CAREGIVER_ID}`,
  );
}

export function getPatientPerformance(patientId) {
  return request(
    `/patients/${patientId}/performance?demo_caregiver_id=${DEMO_CAREGIVER_ID}`,
  );
}
