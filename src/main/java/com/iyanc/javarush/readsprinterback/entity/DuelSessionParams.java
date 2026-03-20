package com.iyanc.javarush.readsprinterback.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores exercise-specific parameters for a DuelSession.
 * Shares the same PK as DuelSession (1:1, @MapsId).
 *
 * Replaces the "polymorphic column reuse" anti-pattern where
 * grid_size / font_size / numbers_sequence in duel_sessions held
 * semantically different values depending on exercise_type.
 *
 * Only columns relevant to the given exercise_type are populated;
 * all others are NULL.
 */
@Entity
@Table(name = "duel_session_params")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DuelSessionParams {

    /** Shares PK with DuelSession. */
    @Id
    private Long sessionId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "session_id")
    private DuelSession session;

    /**
     * Pre-computed at match creation.
     * Used by DuelDbService for OPPONENT_PROGRESS totalCells field
     * and SESSION_RESULT scoring.
     */
    @Column(name = "total_cells")
    private Integer totalCells;

    // ── schulte-table ─────────────────────────────────────────────────────────

    /** Grid dimension, e.g. 5 → 5×5 grid. */
    @Column(name = "grid_size")
    private Integer gridSize;

    /** Cell font size in px. */
    @Column(name = "font_size")
    private Integer fontSize;

    /** JSON int array of shuffled numbers, e.g. "[3,17,5,...]". */
    @Column(name = "schulte_numbers_json", length = 2048)
    private String schulteNumbersJson;

    // ── numbers ───────────────────────────────────────────────────────────────

    /** How many digits each number has (e.g. 3 → numbers like "174"). */
    @Column(name = "digit_count")
    private Integer digitCount;

    /** How long each number is displayed to the player (milliseconds). */
    @Column(name = "display_time_ms")
    private Integer displayTimeMs;

    /** JSON int array of the numbers to memorise, e.g. "[174,392,...]". */
    @Column(name = "numbers_sequence_json", length = 2048)
    private String numbersSequenceJson;

    // ── word-pairs ────────────────────────────────────────────────────────────

    @Column(name = "wp_rows")
    private Integer wpRows;

    @Column(name = "wp_cols")
    private Integer wpCols;

    /** Time limit for the word-pairs exercise in seconds. */
    @Column(name = "wp_time_limit_sec")
    private Integer wpTimeLimitSec;

    @Column(name = "wp_font_size")
    private Integer wpFontSize;

    /** JSON array of {w1, w2, diff} objects. */
    @Column(name = "word_pairs_json", columnDefinition = "TEXT")
    private String wordPairsJson;

    // ── rsvp ─────────────────────────────────────────────────────────────────

    /** FK to the text used for this RSVP session. */
    @Column(name = "rsvp_text_id")
    private Long rsvpTextId;

    /** Number of words shown per syntagm. */
    @Column(name = "rsvp_syntagm_width")
    private Integer rsvpSyntagmWidth;

    /** Milliseconds between syntagm flashes. */
    @Column(name = "rsvp_display_time_ms")
    private Integer rsvpDisplayTimeMs;

    // ── word-search ───────────────────────────────────────────────────────────

    @Column(name = "ws_rows")
    private Integer wsRows;

    @Column(name = "ws_cols")
    private Integer wsCols;

    @Column(name = "ws_word_count")
    private Integer wsWordCount;

    @Column(name = "ws_font_size")
    private Integer wsFontSize;

    /** JSON 2D array of letters: [["к","и","т",...], ...] */
    @Column(name = "ws_grid_json", columnDefinition = "TEXT")
    private String wsGridJson;

    /** JSON array of words to find: ["кивати","дерево",...] */
    @Column(name = "ws_words_json", length = 1024)
    private String wsWordsJson;

    /** JSON array of {word, row, startCol} for client-side highlight */
    @Column(name = "ws_positions_json", length = 2048)
    private String wsPositionsJson;

    // ── letter-search ─────────────────────────────────────────────────────────

    @Column(name = "ls_rows")
    private Integer lsRows;

    @Column(name = "ls_cols")
    private Integer lsCols;

    @Column(name = "ls_letter_count")
    private Integer lsLetterCount;

    /** JSON array of target letters, e.g. ["а","з","м"] */
    @Column(name = "ls_target_letters_json", columnDefinition = "TEXT")
    private String lsTargetLettersJson;

    /** JSON 2D array of single-char strings, e.g. [["а","з",...], ...] */
    @Column(name = "ls_grid_json", columnDefinition = "TEXT")
    private String lsGridJson;
}
