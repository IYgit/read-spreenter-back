package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NumbersQueueRequest extends JoinQueueRequest {

    /** Number of digits per round (3..8) */
    @Min(3) @Max(8)
    private int digitCount = 3;

    /** Number display time in milliseconds (5..1000) */
    @Min(5) @Max(1000)
    private int displayTime = 1000;
}

