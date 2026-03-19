-- ============================================================
-- V11 — Make exercise-specific matchmaking_queue columns nullable.
--       Each queue entry now stores only its own exercise fields.
-- ============================================================

ALTER TABLE matchmaking_queue
    ALTER COLUMN grid_size          DROP NOT NULL,
    ALTER COLUMN font_size          DROP NOT NULL,
    ALTER COLUMN digit_count        DROP NOT NULL,
    ALTER COLUMN display_time       DROP NOT NULL,
    ALTER COLUMN wp_rows            DROP NOT NULL,
    ALTER COLUMN wp_cols            DROP NOT NULL,
    ALTER COLUMN wp_time_limit      DROP NOT NULL,
    ALTER COLUMN wp_font_size       DROP NOT NULL,
    ALTER COLUMN rsvp_syntagm_width DROP NOT NULL,
    ALTER COLUMN rsvp_display_time  DROP NOT NULL;

