package com.iyanc.javarush.readsprinterback.dto.response;

import com.iyanc.javarush.readsprinterback.dto.WordPairDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Sent to BOTH players via /user/queue/duel when a match is found */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MatchFoundMessage {
    private String type;            // "MATCH_FOUND"
    private Long sessionId;
    private String opponentName;
    private String exerciseType;

    // ── Schulte Table ──────────────────────────────────────────────────────────
    private int gridSize;
    private int fontSize;

    // ── Shared: numbers[] ─────────────────────────────────────────────────────
    /** Schulte: shuffled 1..gridSize²; Numbers: array of 10 random numbers */
    private int[] numbers;
    private int totalCells;         // Schulte: gridSize²; Numbers: 10; WordPairs: diffCount

    // ── Numbers exercise ───────────────────────────────────────────────────────
    private int digitCount;         // digits per number
    private int displayTime;        // ms to show each number
    private int totalRounds;        // always 10 for "numbers"

    // ── Word Pairs exercise ────────────────────────────────────────────────────
    private WordPairDto[] pairs;    // full grid of pairs (same for both players)
    private int wpRows;
    private int wpCols;
    private int wpTimeLimit;        // seconds
    private int wpFontSize;
}



