package com.iyanc.javarush.readsprinterback.dto.request;

import lombok.Data;

@Data
public class DuelFinishMessage {
    private Long sessionId;
    private long durationMs;
    private int errors;
    private int score;
    private int progress;
}

