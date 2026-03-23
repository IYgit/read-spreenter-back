-- ============================================================
-- V14 — Word-search word bank table.
--
-- Replaces the hardcoded WORD_BANK constant that existed in
-- both MatchmakingDbService.java (backend) and
-- WordSearchExercise.tsx (frontend), which had already drifted
-- ("парасон" was missing from the Java constant).
-- Single source of truth, consistent with wp_diff_pairs pattern.
-- ============================================================

CREATE TABLE ws_word_bank (
    id   BIGSERIAL PRIMARY KEY,
    word VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO ws_word_bank (word) VALUES
('кивати'),('лопата'),('трава'),  ('музика'),  ('книга'),  ('сонце'),
('вікно'), ('школа'), ('дорога'), ('молоко'),  ('ліжко'),  ('стілець'),
('ранок'), ('вечір'), ('зірка'),  ('берег'),   ('камінь'), ('дерево'),
('квітка'),('вітер'), ('хмара'),  ('місяць'),  ('листок'), ('ягода'),
('робота'),('ліхтар'),('площа'),  ('парасон'), ('ковдра'), ('горіх'),
('калина'),('пшениця'),('вишня'), ('город'),   ('полуниця');

