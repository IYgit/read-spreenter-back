package com.iyanc.javarush.readsprinterback.dto.response;

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
    private int gridSize;
    private int fontSize;
    private int[] numbers;          // shuffled sequence, same for both
    private int totalCells;
}

