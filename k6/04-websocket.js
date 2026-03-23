/**
 * WEBSOCKET DUEL TEST — Read Sprinter
 * ─────────────────────────────────────────────────────────────────────────────
 * Мета: протестувати WebSocket-з'єднання STOMP/SockJS та повний сценарій дуелі:
 *   1. HTTP-авторизація (login)
 *   2. Підключення до WebSocket /ws (raw STOMP без SockJS)
 *   3. STOMP CONNECT з JWT у заголовку Authorization
 *   4. Підписка на /user/queue/duel
 *   5. HTTP POST /api/duels/queue — приєднання до черги
 *   6. Отримання MATCH_FOUND → sessionId
 *   7. Відправка прогресу /app/duel/progress (5 повідомлень)
 *   8. Відправка фінішу /app/duel/finish
 *   9. Відключення
 *
 * Вимога: запускати з 2 VUs, щоб двоє користувачів одночасно потрапили
 *         в чергу та отримали матч.
 *
 * Запуск:
 *   k6 run --vus 2 --iterations 2 k6/04-websocket.js
 * ─────────────────────────────────────────────────────────────────────────────
 */

import http    from 'k6/http';
import ws      from 'k6/ws';
import { check, sleep } from 'k6';
import {
  buildConnectFrame,
  buildSubscribeFrame,
  buildSendFrame,
  buildDisconnectFrame,
  parseFrame,
} from './helpers/stomp.js';
import { register, login, authHeaders, BASE_URL } from './helpers/auth.js';

// WebSocket URL (ws://)
const WS_URL = (BASE_URL.replace('http', 'ws')) + '/ws';

const USER_COUNT = 2;

// ── k6 options ────────────────────────────────────────────────────────────────
export const options = {
  vus:        USER_COUNT,
  iterations: USER_COUNT,   // одна ітерація на VU
  thresholds: {
    ws_connecting:         ['p(95)<3000'],  // час підключення WebSocket < 3 с
    ws_session_duration:   ['p(95)<30000'], // тривалість сесії < 30 с
    http_req_failed:       ['rate<0.01'],
  },
};

// ── Setup: реєстрація та логін двох гравців ───────────────────────────────────
export function setup() {
  const users = [];
  for (let i = 0; i < USER_COUNT; i++) {
    const username = `k6_duel_user_${i}`;
    const email    = `k6_duel_${i}@test.local`;
    const password = 'Duel@1234';
    register(username, email, password);
    const token = login(email, password);
    users.push({ username, token });
  }
  return { users };
}

// ── Основний сценарій ────────────────────────────────────────────────────────
export default function ({ users }) {
  const userIdx = (__VU - 1) % users.length;
  const { token } = users[userIdx];
  const headers   = authHeaders(token);

  let sessionId = null;
  let matched   = false;

  // ── Відкриваємо WebSocket ────────────────────────────────────────────────
  const res = ws.connect(WS_URL, {}, function (socket) {

    // ── Одразу після відкриття: STOMP CONNECT ───────────────────────────
    socket.on('open', () => {
      socket.send(buildConnectFrame(token));
    });

    socket.on('message', (rawData) => {
      const frame = parseFrame(rawData);

      switch (frame.command) {
        // STOMP підключення підтверджено
        case 'CONNECTED': {
          check(frame, { '[WS] STOMP CONNECTED': (f) => f.command === 'CONNECTED' });

          // Підписуємось на особисту чергу дуелей
          socket.send(buildSubscribeFrame('sub-duel', '/user/queue/duel'));

          // Приєднуємось до HTTP-черги матчмейкінгу
          const queueRes = http.post(
            `${BASE_URL}/api/duels/queue`,
            JSON.stringify({ exerciseType: 'schulte-table' }),
            { headers }
          );
          check(queueRes, { '[duels/queue] 200': (r) => r.status === 200 });

          // Якщо матч знайдено одразу (другий гравець вже в черзі)
          const body = queueRes.json();
          if (body && body.status === 'matched') {
            sessionId = body.sessionId;
            matched   = true;
            simulateGame(socket, sessionId, token);
          }
          break;
        }

        // Повідомлення від брокера
        case 'MESSAGE': {
          let payload = {};
          try { payload = JSON.parse(frame.body); } catch (_) {}

          // MATCH_FOUND — матч знайдено через WebSocket
          if (payload.type === 'MATCH_FOUND' && !matched) {
            sessionId = payload.sessionId;
            matched   = true;
            check(payload, {
              '[WS] MATCH_FOUND received':   (p) => !!p.sessionId,
              '[WS] sessionId is number':    (p) => typeof p.sessionId === 'number',
            });
            simulateGame(socket, sessionId, token);
          }

          // DUEL_RESULT — результати дуелі отримано
          if (payload.type === 'DUEL_RESULT') {
            check(payload, { '[WS] DUEL_RESULT received': (p) => !!p });
            // Від'єднуємось після отримання результатів
            socket.send(buildDisconnectFrame());
            socket.close();
          }
          break;
        }

        case 'ERROR': {
          check(frame, { '[WS] no STOMP ERROR': () => false });
          socket.close();
          break;
        }
      }
    });

    socket.on('error', (e) => {
      check(null, { '[WS] no socket error': () => false });
    });

    // Тайм-аут: якщо за 20 секунд матч не знайдено — від'єднуємось
    socket.setTimeout(() => {
      if (!matched) {
        // Виходимо з черги перед закриттям
        http.del(`${BASE_URL}/api/duels/queue`, null, { headers });
      }
      socket.send(buildDisconnectFrame());
      socket.close();
    }, 20000);
  });

  check(res, { '[WS] connection status 101': (r) => r && r.status === 101 });
}

// ── Симуляція гри: прогрес + фініш ───────────────────────────────────────────
function simulateGame(socket, sessionId, _token) {
  // Надсилаємо 5 повідомлень прогресу з інтервалом ~1 с
  let step = 0;
  const interval = socket.setInterval(() => {
    step++;
    socket.send(buildSendFrame('/app/duel/progress', {
      sessionId,
      progress: step * 5,
      errors:   0,
    }));

    if (step >= 5) {
      socket.clearInterval(interval);

      sleep(0.5);

      // Відправляємо FINISH
      socket.send(buildSendFrame('/app/duel/finish', {
        sessionId,
        durationMs:   5000 + Math.floor(Math.random() * 3000),
        errors:       Math.floor(Math.random() * 3),
        score:        randomInt(70, 100),
        progress:     25,
      }));
    }
  }, 1000);
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

