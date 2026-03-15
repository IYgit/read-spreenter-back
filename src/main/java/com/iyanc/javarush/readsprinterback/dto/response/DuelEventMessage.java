package com.iyanc.javarush.readsprinterback.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Generic duel event broadcast to /topic/duel/{sessionId} */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DuelEventMessage {

    private String type;
    // COUNTDOWN | START | OPPONENT_PROGRESS | OPPONENT_FINISHED
    // OPPONENT_DISCONNECTED | OPPONENT_LEFT | SESSION_RESULT

    // for COUNTDOWN
    private Integer countdown;

    // for OPPONENT_PROGRESS
    private String opponentName;
    private Integer opponentProgress;
    private Integer totalCells;

    // for OPPONENT_FINISHED
    private Long opponentDurationMs;
    private Integer opponentErrors;
    private Integer opponentScore;

    // for SESSION_RESULT (both players finished)
    private ParticipantResult myResult;
    private ParticipantResult opponentResult;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParticipantResult {
        private String username;
        private Long durationMs;
        private int errors;
        private int score;
        private int progress;
        private int totalCells;
        private boolean finished;
        private boolean disconnected;
    }
}

