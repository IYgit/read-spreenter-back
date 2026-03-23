-- ============================================================
-- V6 — Add 'numbers' exercise fields to matchmaking_queue
-- ============================================================

ALTER TABLE matchmaking_queue
    ADD COLUMN digit_count  INT NOT NULL DEFAULT 3,
    ADD COLUMN display_time INT NOT NULL DEFAULT 1000;

