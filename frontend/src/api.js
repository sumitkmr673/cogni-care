const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";
const DEMO_CAREGIVER_EMAIL =
  import.meta.env.VITE_DEMO_CAREGIVER_EMAIL ||
  "demo.caregiver@cogni-care.example";
const DEMO_CAREGIVER_PASSWORD =
  import.meta.env.VITE_DEMO_CAREGIVER_PASSWORD || "DemoCaregiverOnly-2026!";

let accessTokenPromise = null;

async function getAccessToken() {
  if (!accessTokenPromise) {
    accessTokenPromise = loginForAccessToken();
  }
  return accessTokenPromise;
}

async function loginForAccessToken() {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email: DEMO_CAREGIVER_EMAIL,
      password: DEMO_CAREGIVER_PASSWORD,
    }),
  });
  if (!response.ok) {
    accessTokenPromise = null;
    throw new Error("Unable to authenticate the caregiver session");
  }
  const body = await response.json();
  return body.access_token;
}

async function request(path, { retry = true } = {}) {
  const token = await getAccessToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (response.status === 401 && retry) {
    accessTokenPromise = null;
    return request(path, { retry: false });
  }
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
  return request("/patients");
}

export function getPatientDashboard(patientId) {
  return request(`/patients/${patientId}/dashboard`);
}

export function getPatientPerformance(patientId) {
  return request(`/patients/${patientId}/performance`);
}
