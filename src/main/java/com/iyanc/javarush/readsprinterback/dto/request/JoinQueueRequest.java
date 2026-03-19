package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinQueueRequest {

    /** "schulte-table" or "numbers" */
    @NotBlank
    private String exerciseType;

    // ── Schulte Table parameters ──────────────────────────────────────────────

    @Min(3) @Max(7)
    private int gridSize = 5;

    @Min(12) @Max(40)
    private int fontSize = 20;

    // ── Numbers exercise parameters ───────────────────────────────────────────

    /** Number of digits per round (3..8) */
    @Min(3) @Max(8)
    private int digitCount = 3;

    /** Number display time in milliseconds (50..2000) */
    @Min(50) @Max(2000)
    private int displayTime = 1000;

    // ── Word Pairs exercise parameters ────────────────────────────────────────

    /** Grid rows (3..5) */
    @Min(3) @Max(5)
    private int wpRows = 4;

    /** Grid cols (3..5) */
    @Min(3) @Max(5)
    private int wpCols = 4;

    /** Time limit in seconds (30..120) */
    @Min(30) @Max(120)
    private int wpTimeLimit = 60;

    /** For "word-pairs" exercise: font size in px (12..18) */
    @Min(12) @Max(18)
    private int wpFontSize = 14;

    // ── RSVP exercise parameters ──────────────────────────────────────────────

    /** Number of words per syntagm (1..5) */
    @Min(1) @Max(5)
    private int rsvpSyntagmWidth = 3;

    /** Display time per syntagm in milliseconds (100..1000) */
    @Min(100) @Max(1000)
    private int rsvpDisplayTime = 300;
}



