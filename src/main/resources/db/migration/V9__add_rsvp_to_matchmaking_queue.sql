-- ============================================================
-- V9 — Add 'rsvp' exercise fields to matchmaking_queue
-- ============================================================

ALTER TABLE matchmaking_queue
    ADD COLUMN rsvp_syntagm_width INT NOT NULL DEFAULT 3,
    ADD COLUMN rsvp_display_time  INT NOT NULL DEFAULT 300;

