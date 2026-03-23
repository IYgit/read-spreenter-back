/**
 * STRESS TEST — Read Sprinter REST API
 * ─────────────────────────────────────────────────────────────────────────────
 * Мета: знайти межу продуктивності — точку, в якій система деградує або
 *       починає повертати помилки.
 * Сценарій: агресивне ступінчасте наростання до 100 VU → спуск.
 * Запуск:
 *   k6 run k6/03-stress.js
 * ─────────────────────────────────────────────────────────────────────────────
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { register, login, authHeaders, BASE_URL } from './helpers/auth.js';

const USER_COUNT = 20; // пул користувачів (токени перевикористовуються циклічно)

// ── k6 options ────────────────────────────────────────────────────────────────
export const options = {
  stages: [
    { duration: '30s', target: 10  },   // розгін
    { duration: '1m',  target: 30  },   // нормальне навантаження
    { duration: '1m',  target: 60  },   // підвищене навантаження
    { duration: '1m',  target: 100 },   // стрес-піке
    { duration: '30s', target: 0   },   // відновлення
  ],
  thresholds: {
    // Попереджувальні пороги (тест НЕ провалюється при перевищенні,
    // але результати фіксуються у звіті)
    http_req_failed:   ['rate<0.05'],   // допускаємо до 5 % помилок під стресом
    http_req_duration: ['p(95)<3000'],  // 95-й перцентиль < 3 с
    http_req_duration: ['p(99)<5000'],  // 99-й перцентиль < 5 с
  },
};

// ── Setup ─────────────────────────────────────────────────────────────────────
export function setup() {
  const users = [];
  for (let i = 0; i < USER_COUNT; i++) {
    const username = `k6_stress_user_${i}`;
    const email    = `k6_stress_${i}@test.local`;
    const password = 'Stress@1234';
    register(username, email, password);
    const token = login(email, password);
    users.push({ token });
  }
  return { users };
}

// ── Основний сценарій ────────────────────────────────────────────────────────
export default function ({ users }) {
  const { token } = users[(__VU - 1) % users.length];
  const headers   = authHeaders(token);

  // Акцент на найнавантаженіших ендпоїнтах
  const requests = [
    // Пакетний запит (batch) — паралельно
    ['GET', `${BASE_URL}/api/texts`,                    null,    {}],
    ['GET', `${BASE_URL}/api/word-pairs?rows=6&cols=6`, null,    { headers }],
    ['GET', `${BASE_URL}/api/word-search/words`,        null,    { headers }],
  ];

  // Виконуємо пакет паралельно
  const responses = http.batch(
    requests.map(([method, url, body, params]) => ({
      method,
      url,
      body,
      params: { ...params, tags: { batch: 'true' } },
    }))
  );
  responses.forEach((res, i) => {
    check(res, { [`batch[${i}] not 5xx`]: (r) => r.status < 500 });
  });

  sleep(randomBetween(0.1, 0.5));

  // Послідовний запис результату
  const res = http.post(
    `${BASE_URL}/api/results`,
    JSON.stringify({
      exerciseType: 'rsvp',
      score:        randomInt(40, 100),
      durationSec:  randomInt(20, 90),
      wpm:          randomInt(100, 600),
      correctCount: randomInt(8, 20),
      totalCount:   20,
    }),
    { headers, tags: { endpoint: 'results-write' } }
  );
  check(res, { '[results POST] not 5xx': (r) => r.status < 500 });

  sleep(randomBetween(0.2, 0.8));
}

// ── Утиліти ──────────────────────────────────────────────────────────────────
function randomBetween(min, max) {
  return Math.random() * (max - min) + min;
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

