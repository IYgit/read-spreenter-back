-- ============================================================
-- V13 — Add word-search exercise support to duel tables.
-- ============================================================

-- ── matchmaking_queue: player preferences ────────────────────
ALTER TABLE matchmaking_queue
    ADD COLUMN ws_rows       INT,
    ADD COLUMN ws_cols       INT,
    ADD COLUMN ws_word_count INT,
    ADD COLUMN ws_font_size  INT;

-- ── duel_session_params: session data ────────────────────────
ALTER TABLE duel_session_params
    ADD COLUMN ws_rows           INT,
    ADD COLUMN ws_cols           INT,
    ADD COLUMN ws_word_count     INT,
    ADD COLUMN ws_font_size      INT,
    ADD COLUMN ws_grid_json      TEXT,
    ADD COLUMN ws_words_json     TEXT,
    ADD COLUMN ws_positions_json TEXT;

