/**
 * LOAD TEST — Read Sprinter REST API
 * ─────────────────────────────────────────────────────────────────────────────
 * Мета: змоделювати очікуване реальне навантаження.
 * Сценарій: плавне наростання до 20 VU → утримання 3 хв → спуск.
 * Запуск:
 *   k6 run k6/02-load.js
 *   k6 run -e USERS=20 k6/02-load.js
 * ─────────────────────────────────────────────────────────────────────────────
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { register, login, authHeaders, BASE_URL } from './helpers/auth.js';

// Кількість тестових користувачів (налаштовується через env)
const USER_COUNT = parseInt(__ENV.USERS || '10');

// ── k6 options ────────────────────────────────────────────────────────────────
export const options = {
  stages: [
    { duration: '1m',  target: USER_COUNT },      // наростання
    { duration: '3m',  target: USER_COUNT },      // стале навантаження
    { duration: '30s', target: 0 },               // спуск
  ],
  thresholds: {
    http_req_failed:              ['rate<0.01'],   // < 1 % помилок
    http_req_duration:            ['p(95)<1500'],  // 95 % запитів < 1.5 с
    'http_req_duration{endpoint:texts}':    ['p(95)<800'],
    'http_req_duration{endpoint:results}':  ['p(95)<1200'],
    'http_req_duration{endpoint:login}':    ['p(95)<1000'],
  },
};

// ── Setup: реєстрація пула користувачів ──────────────────────────────────────
export function setup() {
  const users = [];
  for (let i = 0; i < USER_COUNT; i++) {
    const username = `k6_load_user_${i}`;
    const email    = `k6_load_${i}@test.local`;
    const password = 'Load@1234';
    register(username, email, password);
    const token = login(email, password);
    users.push({ username, token });
  }
  return { users };
}

// ── Основний сценарій ────────────────────────────────────────────────────────
export default function ({ users }) {
  // Кожен VU отримує свого користувача за циклічним індексом
  const { token } = users[(__VU - 1) % users.length];
  const headers   = authHeaders(token);

  // ── Сценарій: читання та вправи ──────────────────────────────────────────

  // 1. Список текстів
  let res = http.get(`${BASE_URL}/api/texts`, {
    headers,
    tags: { endpoint: 'texts' },
  });
  check(res, { '[texts] 200': (r) => r.status === 200 });
  sleep(randomBetween(0.5, 1.5));

  // 2. Перший текст
  const texts = res.json();
  if (texts && texts.length > 0) {
    const textId = texts[0].id;
    res = http.get(`${BASE_URL}/api/texts/${textId}`, {
      headers,
      tags: { endpoint: 'texts' },
    });
    check(res, { '[texts/:id] 200': (r) => r.status === 200 });
    sleep(randomBetween(0.3, 0.8));
  }

  // 3. Word pairs
  res = http.get(`${BASE_URL}/api/word-pairs?rows=4&cols=4`, {
    headers,
    tags: { endpoint: 'word-pairs' },
  });
  check(res, { '[word-pairs] 200': (r) => r.status === 200 });
  sleep(randomBetween(0.5, 1.0));

  // 4. Word search words
  res = http.get(`${BASE_URL}/api/word-search/words`, {
    headers,
    tags: { endpoint: 'word-search' },
  });
  check(res, { '[word-search] 200': (r) => r.status === 200 });
  sleep(randomBetween(0.5, 1.0));

  // 5. Збереження результату
  res = http.post(
    `${BASE_URL}/api/results`,
    JSON.stringify({
      exerciseType: pickRandom(['rsvp', 'schulte-table', 'word-pairs', 'syntagm-reading']),
      score:        randomInt(50, 100),
      durationSec:  randomInt(30, 120),
      wpm:          randomInt(150, 500),
      correctCount: randomInt(10, 20),
      totalCount:   20,
    }),
    {
      headers,
      tags: { endpoint: 'results' },
    }
  );
  check(res, { '[results POST] 201': (r) => r.status === 201 });
  sleep(randomBetween(0.3, 0.8));

  // 6. Статистика
  res = http.get(`${BASE_URL}/api/results/me/summary`, {
    headers,
    tags: { endpoint: 'results' },
  });
  check(res, { '[results/me/summary] 200': (r) => r.status === 200 });

  sleep(randomBetween(1.0, 2.0));
}

// ── Утиліти ──────────────────────────────────────────────────────────────────
function randomBetween(min, max) {
  return Math.random() * (max - min) + min;
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pickRandom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

