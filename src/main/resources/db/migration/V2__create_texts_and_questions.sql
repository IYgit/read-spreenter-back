-- V2: Create texts and questions tables
CREATE TABLE texts
(
    id         BIGSERIAL PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    difficulty VARCHAR(10)  NOT NULL CHECK (difficulty IN ('easy', 'medium', 'hard')),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE questions
(
    id            BIGSERIAL PRIMARY KEY,
    text_id       BIGINT       NOT NULL REFERENCES texts (id) ON DELETE CASCADE,
    question_text TEXT         NOT NULL,
    correct_index INT          NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE question_options
(
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT       NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    option_text VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_questions_text_id ON questions (text_id);
CREATE INDEX idx_question_options_question_id ON question_options (question_id);

