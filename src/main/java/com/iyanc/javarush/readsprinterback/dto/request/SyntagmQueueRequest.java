package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SyntagmQueueRequest extends JoinQueueRequest {

    /** Number of words per syntagm (1..6) */
    @Min(1) @Max(6)
    private int syntagmWidth = 2;

    /** Display time per syntagm in milliseconds (100..1000) */
    @Min(100) @Max(1000)
    private int displayTime = 500;
}

