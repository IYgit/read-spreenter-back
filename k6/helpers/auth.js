import http from 'k6/http';
import { check } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8000';

/**
 * Registers a new user. Returns parsed response body.
 * Ignores 409 Conflict (user already exists).
 */
export function register(username, email, password) {
  const res = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ username, email, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  // 201 = created, 409 = already exists (ok for repeated test runs)
  check(res, { 'register: 201 or 409': (r) => r.status === 201 || r.status === 409 });
  return res.json();
}

/**
 * Logs in and returns the access token string.
 * @param {string} email - user's email (LoginRequest expects "email" field)
 */
export function login(email, password) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'login: status 200': (r) => r.status === 200 });
  const body = res.json();
  return body.accessToken;
}

/**
 * Returns JSON + Authorization headers for authenticated requests.
 */
export function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}

