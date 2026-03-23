-- ============================================================
-- V7 — Add 'word-pairs' exercise fields to matchmaking_queue
-- ============================================================

ALTER TABLE matchmaking_queue
    ADD COLUMN wp_rows       INT NOT NULL DEFAULT 4,
    ADD COLUMN wp_cols       INT NOT NULL DEFAULT 4,
    ADD COLUMN wp_time_limit INT NOT NULL DEFAULT 60,
    ADD COLUMN wp_font_size  INT NOT NULL DEFAULT 14;

