const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";
const TOKEN_KEY = "cogni-care-access-token";

function getToken() {
  return window.localStorage.getItem(TOKEN_KEY);
}

export function hasToken() {
  return Boolean(getToken());
}

function setToken(token) {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_KEY);
}

async function parseError(response) {
  let detail = `Request failed with status ${response.status}`;
  try {
    const body = await response.json();
    detail = body.detail || detail;
  } catch {
    // Keep the HTTP error when the backend does not return JSON.
  }
  const error = new Error(detail);
  error.status = response.status;
  return error;
}

async function request(path, options = {}) {
  const token = getToken();
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  if (!response.ok) {
    throw await parseError(response);
  }
  return response.status === 204 ? null : response.json();
}

export async function login(email, password) {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) {
    throw await parseError(response);
  }
  const body = await response.json();
  setToken(body.access_token);
  return getCurrentUser();
}

export function getCurrentUser() {
  return request("/auth/me");
}

export function getPatients() {
  return request("/patients");
}

export function getPatientDashboard(patientId) {
  return request(`/patients/${patientId}/dashboard`);
}

export function getPatientTrends(patientId) {
  return request(`/patients/${patientId}/trends`);
}

export function getPatientSessions(patientId, limit = 50) {
  return request(`/patients/${patientId}/sessions?limit=${limit}`);
}

export function getPatientReminders(patientId) {
  return request(`/patients/${patientId}/reminders`);
}

export function createPatientReminder(patientId, reminder) {
  return request(`/patients/${patientId}/reminders`, {
    method: "POST",
    body: JSON.stringify(reminder),
  });
}
