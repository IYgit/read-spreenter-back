-- V4: Create exercise_results table
CREATE TABLE exercise_results
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_type VARCHAR(50) NOT NULL,
    text_id       BIGINT               REFERENCES texts (id) ON DELETE SET NULL,
    score         INT,
    duration_sec  INT,
    wpm           INT,
    correct_count INT,
    total_count   INT,
    extra_data    JSONB,
    completed_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exercise_results_user_id ON exercise_results (user_id);
CREATE INDEX idx_exercise_results_exercise_type ON exercise_results (exercise_type);
CREATE INDEX idx_exercise_results_completed_at ON exercise_results (completed_at);

