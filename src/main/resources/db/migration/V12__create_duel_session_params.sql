-- ============================================================
-- V12 — Normalize duel session parameters into a dedicated table.
--
-- Resolves technical debt: "polymorphic column reuse" anti-pattern in
-- duel_sessions. Previously grid_size / font_size / numbers_sequence
-- held entirely different values depending on exercise_type.
-- Now each exercise type has its own semantically named columns.
-- ============================================================

CREATE TABLE duel_session_params (
    session_id              BIGINT  PRIMARY KEY REFERENCES duel_sessions(id) ON DELETE CASCADE,

    -- Universally pre-computed at match creation, used for progress display.
    total_cells             INT,

    -- ── schulte-table ──────────────────────────────────────────────────────
    grid_size               INT,          -- grid dimension, e.g. 5 → 5×5 grid
    font_size               INT,          -- cell font size in px
    schulte_numbers_json    TEXT,         -- JSON int array, e.g. "[3,17,5,...]"

    -- ── numbers ───────────────────────────────────────────────────────────
    digit_count             INT,          -- digits per number (e.g. 3 → "174")
    display_time_ms         INT,          -- how long each number is shown (ms)
    numbers_sequence_json   TEXT,         -- JSON int array of numbers to memorise

    -- ── word-pairs ─────────────────────────────────────────────────────────
    wp_rows                 INT,
    wp_cols                 INT,
    wp_time_limit_sec       INT,
    wp_font_size            INT,
    word_pairs_json         TEXT,         -- JSON array of {w1, w2, diff}

    -- ── rsvp ──────────────────────────────────────────────────────────────
    rsvp_text_id            BIGINT REFERENCES texts(id),
    rsvp_syntagm_width      INT,
    rsvp_display_time_ms    INT
);

-- ── Migrate existing schulte-table sessions ──────────────────────────────────
INSERT INTO duel_session_params
    (session_id, total_cells, grid_size, font_size, schulte_numbers_json)
SELECT
    id,
    grid_size * grid_size,
    grid_size,
    font_size,
    numbers_sequence
FROM duel_sessions
WHERE exercise_type = 'schulte-table';

-- ── Migrate existing numbers sessions ───────────────────────────────────────
-- grid_size column held digitCount, font_size held displayTime (ms).
INSERT INTO duel_session_params
    (session_id, total_cells, digit_count, display_time_ms, numbers_sequence_json)
SELECT
    id,
    10,           -- NUMBERS_TOTAL_ROUNDS constant in MatchmakingDbService
    grid_size,    -- was digitCount
    font_size,    -- was displayTime ms
    numbers_sequence
FROM duel_sessions
WHERE exercise_type = 'numbers';

-- ── Migrate existing word-pairs sessions ─────────────────────────────────────
-- grid_size column held rows*10 + cols, font_size held wpFontSize.
-- wp_time_limit was never persisted before → NULL.
INSERT INTO duel_session_params
    (session_id, total_cells, wp_rows, wp_cols, wp_time_limit_sec, wp_font_size, word_pairs_json)
SELECT
    id,
    (grid_size / 10) * (grid_size % 10) / 2,   -- approx 50% diff pairs
    grid_size / 10,                             -- rows
    grid_size % 10,                             -- cols
    NULL,                                       -- was not persisted
    font_size,                                  -- was wpFontSize
    numbers_sequence
FROM duel_sessions
WHERE exercise_type = 'word-pairs';

-- ── Migrate existing rsvp sessions ───────────────────────────────────────────
-- grid_size held textId, font_size held rsvpDisplayTimeMs,
-- numbers_sequence held syntagmWidth as a plain integer string.
INSERT INTO duel_session_params
    (session_id, total_cells, rsvp_text_id, rsvp_syntagm_width, rsvp_display_time_ms)
SELECT
    s.id,
    (SELECT COUNT(*) FROM questions q WHERE q.text_id = s.grid_size),
    s.grid_size,                              -- was textId
    CAST(s.numbers_sequence AS INTEGER),      -- was syntagmWidth stored as "3"
    s.font_size                               -- was displayTimeMs
FROM duel_sessions s
WHERE s.exercise_type = 'rsvp';

-- ── Drop the now-redundant columns from duel_sessions ────────────────────────
ALTER TABLE duel_sessions
    DROP COLUMN grid_size,
    DROP COLUMN font_size,
    DROP COLUMN numbers_sequence;

