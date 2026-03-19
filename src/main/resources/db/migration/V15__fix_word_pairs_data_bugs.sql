-- ============================================================
-- V15 — Fix word-pairs dictionary data bugs.
--
-- Two classes of bugs found in V8 seed data:
--   1. Five rows in wp_diff_pairs where word1 = word2
--      (identical words marked as "different")
--   2. One row with Latin homoglyphs disguised as Cyrillic:
--      'моркva' — 'v' is U+0076 LATIN, 'a' is U+0061 LATIN,
--      visually identical to Cyrillic 'в' (U+0432) / 'а' (U+0430)
--
-- Fixes:
--   - DELETE all identical pairs (word1 = word2)
--   - DELETE all pairs containing non-Cyrillic/non-space characters
--   - INSERT correct replacement pairs
--   - ADD CHECK constraint to prevent regression
-- ============================================================

-- ── Step 1: Remove identical pairs (word1 = word2) ──────────
DELETE FROM wp_diff_pairs WHERE word1 = word2;

-- ── Step 2: Remove mixed-script (Latin homoglyphs) pairs ────
-- Matches any row where word1 or word2 contains a Latin letter
DELETE FROM wp_diff_pairs
WHERE word1 ~ '[a-zA-Z]'
   OR word2 ~ '[a-zA-Z]';

-- ── Step 3: Insert correct replacement pairs ─────────────────
INSERT INTO wp_diff_pairs (word1, word2) VALUES
-- replaces ('варення', 'варення')
('варення',  'варіння'),
-- replaces ('суниця', 'суниця')
('суниця',   'куниця'),
-- replaces ('перець', 'перець')
('перець',   'кінець'),
-- replaces ('бігти', 'бігти')
('бігти',    'лігти'),
-- replaces ('сміятись', 'сміятись')
('сміятись', 'сміялись'),
-- replaces ('моркva', 'морква')  — was Latin-in-Cyrillic homoglyph
('морква',   'моркав');

-- ── Step 4: Add CHECK constraint to prevent future regression ─
ALTER TABLE wp_diff_pairs
    ADD CONSTRAINT chk_wp_diff_words_not_equal CHECK (word1 <> word2);

