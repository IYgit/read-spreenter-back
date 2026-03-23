/**
 * SMOKE TEST — Read Sprinter REST API
 * ─────────────────────────────────────────────────────────────────────────────
 * Мета: переконатися, що всі ключові ендпоїнти відповідають без помилок.
 * Налаштування: 1 VU, 1 ітерація.
 * Запуск:
 *   k6 run k6/01-smoke.js
 *   k6 run -e TEST_USER=myuser -e TEST_PASS=mypassword k6/01-smoke.js
 * ─────────────────────────────────────────────────────────────────────────────
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { register, login, authHeaders, BASE_URL } from './helpers/auth.js';

// ── Test user credentials (override via env vars) ────────────────────────────
const USERNAME = __ENV.TEST_USER || 'k6_smoke_user';
const EMAIL    = __ENV.TEST_EMAIL || 'k6_smoke@test.local';
const PASSWORD = __ENV.TEST_PASS  || 'Smoke@1234';

// ── k6 options ────────────────────────────────────────────────────────────────
export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    // 0 % of requests may fail
    http_req_failed: ['rate<0.01'],
    // 95-й перцентиль часу відповіді < 2 с
    http_req_duration: ['p(95)<2000'],
  },
};

// ── Setup: register (idempotent) & login ──────────────────────────────────────
export function setup() {
  register(USERNAME, EMAIL, PASSWORD);
  const token = login(EMAIL, PASSWORD);
  return { token };
}

// ── Main test scenario ────────────────────────────────────────────────────────
export default function ({ token }) {
  const headers = authHeaders(token);

  // 1. GET /api/auth/me
  let res = http.get(`${BASE_URL}/api/auth/me`, { headers });
  check(res, { '[auth/me] status 200': (r) => r.status === 200 });
  sleep(0.3);

  // 2. GET /api/texts
  res = http.get(`${BASE_URL}/api/texts`);
  check(res, {
    '[texts] status 200': (r) => r.status === 200,
    '[texts] is array':   (r) => Array.isArray(r.json()),
  });
  sleep(0.3);

  // 3. GET /api/word-pairs?rows=4&cols=4
  res = http.get(`${BASE_URL}/api/word-pairs?rows=4&cols=4`, { headers });
  check(res, {
    '[word-pairs] status 200': (r) => r.status === 200,
    '[word-pairs] 16 cells':   (r) => r.json().length === 16,
  });
  sleep(0.3);

  // 4. GET /api/word-search/words
  res = http.get(`${BASE_URL}/api/word-search/words`, { headers });
  check(res, {
    '[word-search] status 200':   (r) => r.status === 200,
    '[word-search] non-empty':    (r) => r.json().length > 0,
  });
  sleep(0.3);

  // 5. POST /api/results  (save exercise result)
  res = http.post(
    `${BASE_URL}/api/results`,
    JSON.stringify({
      exerciseType: 'rsvp',
      score:        85,
      durationSec:  60,
      wpm:          300,
      correctCount: 17,
      totalCount:   20,
    }),
    { headers }
  );
  check(res, { '[results POST] status 201': (r) => r.status === 201 });
  sleep(0.3);

  // 6. GET /api/results/me
  res = http.get(`${BASE_URL}/api/results/me`, { headers });
  check(res, { '[results/me] status 200': (r) => r.status === 200 });
  sleep(0.3);

  // 7. GET /api/results/me/summary
  res = http.get(`${BASE_URL}/api/results/me/summary`, { headers });
  check(res, { '[results/me/summary] status 200': (r) => r.status === 200 });
  sleep(0.3);

  // 8. GET /api/results/me/type/rsvp
  res = http.get(`${BASE_URL}/api/results/me/type/rsvp`, { headers });
  check(res, { '[results/me/type/rsvp] status 200': (r) => r.status === 200 });
  sleep(0.3);

  // 9. POST /api/duels/queue  → join, then immediately leave
  res = http.post(
    `${BASE_URL}/api/duels/queue`,
    JSON.stringify({ exerciseType: 'schulte-table' }),
    { headers }
  );
  check(res, { '[duels/queue POST] status 200': (r) => r.status === 200 });
  sleep(0.3);

  res = http.del(`${BASE_URL}/api/duels/queue`, null, { headers });
  check(res, { '[duels/queue DELETE] status 204': (r) => r.status === 204 });
}

