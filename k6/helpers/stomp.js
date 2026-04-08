/**
 * Minimal STOMP-over-WebSocket frame builder/parser for k6.
 * Implements STOMP 1.2 framing (command\nheaders\n\nbody\x00).
 */

/**
 * Builds a STOMP CONNECT frame with JWT authentication.
 */
export function buildConnectFrame(token) {
  return `CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer ${token}\n\n\x00`;
}

/**
 * Builds a STOMP SUBSCRIBE frame.
 * @param {string} id - Unique subscription ID (e.g. "sub-0")
 * @param {string} destination - e.g. "/user/queue/duel"
 */
export function buildSubscribeFrame(id, destination) {
  return `SUBSCRIBE\nid:${id}\ndestination:${destination}\nack:auto\n\n\x00`;
}

/**
 * Builds a STOMP SEND frame with a JSON body.
 * @param {string} destination - e.g. "/app/duel/progress"
 * @param {Object} body - JavaScript object that will be JSON-serialised
 */
export function buildSendFrame(destination, body) {
  const json = JSON.stringify(body);
  return `SEND\ndestination:${destination}\ncontent-type:application/json\ncontent-length:${json.length}\n\n${json}\x00`;
}

/**
 * Builds a STOMP DISCONNECT frame.
 */
export function buildDisconnectFrame() {
  return `DISCONNECT\n\n\x00`;
}

/**
 * Parses a raw STOMP frame string into { command, headers, body }.
 * Returns { command: 'HEARTBEAT' } for heartbeat frames (\n).
 */
export function parseFrame(raw) {
  if (!raw || raw === '\n') return { command: 'HEARTBEAT', headers: {}, body: '' };

  const nullIdx = raw.indexOf('\x00');
  const frame = nullIdx !== -1 ? raw.substring(0, nullIdx) : raw;
  const lines = frame.split('\n');

  const command = lines[0].trim();
  const headers = {};
  let i = 1;
  while (i < lines.length && lines[i].trim() !== '') {
    const colonIdx = lines[i].indexOf(':');
    if (colonIdx !== -1) {
      const key = lines[i].substring(0, colonIdx).trim();
      const val = lines[i].substring(colonIdx + 1).trim();
      headers[key] = val;
    }
    i++;
  }
  const body = lines.slice(i + 1).join('\n').trim();
  return { command, headers, body };
}

