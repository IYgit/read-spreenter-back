package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SchulteQueueRequest extends JoinQueueRequest {

    /** Grid size N×N (3..7) */
    @Min(3) @Max(7)
    private int gridSize = 5;

    /** Cell font size in px (12..40) */
    @Min(12) @Max(40)
    private int fontSize = 20;
}

