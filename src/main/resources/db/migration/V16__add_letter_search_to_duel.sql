-- ============================================================
-- V16 — Add letter-search exercise support to duel tables.
-- ============================================================

-- ── matchmaking_queue: player preferences ────────────────────
ALTER TABLE matchmaking_queue
    ADD COLUMN ls_rows         INT,
    ADD COLUMN ls_cols         INT,
    ADD COLUMN ls_letter_count INT;

-- ── duel_session_params: session data ────────────────────────
ALTER TABLE duel_session_params
    ADD COLUMN ls_rows                INT,
    ADD COLUMN ls_cols                INT,
    ADD COLUMN ls_letter_count        INT,
    ADD COLUMN ls_target_letters_json TEXT,
    ADD COLUMN ls_grid_json           TEXT;

