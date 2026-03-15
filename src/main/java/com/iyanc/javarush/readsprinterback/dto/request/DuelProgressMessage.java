package com.iyanc.javarush.readsprinterback.dto.request;

import lombok.Data;

@Data
public class DuelProgressMessage {
    private Long sessionId;
    private int progress;   // numbers found so far
    private int errors;
}

