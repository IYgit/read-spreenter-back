-- ============================================================
-- V5 — Duel sessions, participants, matchmaking queue
-- ============================================================

CREATE TABLE matchmaking_queue (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    exercise_type   VARCHAR(50) NOT NULL DEFAULT 'schulte-table',
    grid_size       INT         NOT NULL DEFAULT 5,
    font_size       INT         NOT NULL DEFAULT 20,
    joined_at       TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE duel_sessions (
    id               BIGSERIAL PRIMARY KEY,
    exercise_type    VARCHAR(50)  NOT NULL DEFAULT 'schulte-table',
    grid_size        INT          NOT NULL,
    font_size        INT          NOT NULL,
    numbers_sequence TEXT         NOT NULL,   -- JSON array, e.g. "[3,17,5,...]"
    status           VARCHAR(20)  NOT NULL DEFAULT 'WAITING',
                                              -- WAITING | COUNTDOWN | ACTIVE | FINISHED
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    started_at       TIMESTAMP,
    finished_at      TIMESTAMP
);

CREATE TABLE duel_participants (
    id           BIGSERIAL PRIMARY KEY,
    session_id   BIGINT    NOT NULL REFERENCES duel_sessions(id) ON DELETE CASCADE,
    user_id      BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    progress     INT       NOT NULL DEFAULT 0,   -- how many numbers found
    errors       INT       NOT NULL DEFAULT 0,
    duration_ms  BIGINT,                         -- null until finished
    score        INT,
    finished     BOOLEAN   NOT NULL DEFAULT FALSE,
    finished_at  TIMESTAMP,
    disconnected BOOLEAN   NOT NULL DEFAULT FALSE,
    UNIQUE (session_id, user_id)
);

-- Index for fast matchmaking lookup
CREATE INDEX idx_matchmaking_queue_exercise ON matchmaking_queue(exercise_type, joined_at);
-- Index for session participant lookup
CREATE INDEX idx_duel_participants_session ON duel_participants(session_id);
CREATE INDEX idx_duel_participants_user    ON duel_participants(user_id);

