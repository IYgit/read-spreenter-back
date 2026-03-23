package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Queue request for the Letter Search duel exercise. */
@Data
@EqualsAndHashCode(callSuper = true)
public class LetterSearchQueueRequest extends JoinQueueRequest {

    /**
     * Grid size index (0..3):
     *   0 → 8×10 = 80 cells
     *   1 → 9×10 = 90 cells
     *   2 → 10×12 = 120 cells
     *   3 → 11×14 = 154 cells
     */
    @Min(0) @Max(3)
    private int gridSizeIdx = 1;

    /** Number of distinct target letters to find (1..4). */
    @Min(1) @Max(4)
    private int letterCount = 2;
}

