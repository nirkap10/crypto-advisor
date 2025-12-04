const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Minimal helper to call the Spring backend with HTTP Basic auth.
 * Stores credentials in memory only (not secure for production, fine for demo).
 */
export function apiFetch(path, { auth, method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth?.username && auth?.password) {
    const token = btoa(`${auth.username}:${auth.password}`);
    headers.Authorization = `Basic ${token}`;
  }
  return fetch(`${API_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  }).then(async (res) => {
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || `Request failed (${res.status})`);
    }
    const ct = res.headers.get('content-type') || '';
    return ct.includes('application/json') ? res.json() : res.text();
  });
}

export const api = {
  register: (payload) => apiFetch('/api/auth/register', { method: 'POST', body: payload }),
  login: (payload) => apiFetch('/api/auth/login', { method: 'POST', body: payload }),
  getPreferenceOptions: () => apiFetch('/api/preferences/options'),
  getPreferences: (auth) => apiFetch('/api/preferences/me', { auth }),
  savePreferences: (auth, payload) => apiFetch('/api/preferences/me', { auth, method: 'POST', body: payload }),
  dashboardToday: (auth) => apiFetch('/api/dashboard/today', { auth }),
  vote: (auth, snapshotId, payload) =>
    apiFetch(`/api/dashboard/${snapshotId}/feedback`, { auth, method: 'POST', body: payload }),
};
