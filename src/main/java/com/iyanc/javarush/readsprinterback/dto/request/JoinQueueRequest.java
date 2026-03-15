package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinQueueRequest {

    /** "schulte-table" or "any" */
    @NotBlank
    private String exerciseType;

    @Min(3) @Max(7)
    private int gridSize = 5;

    @Min(12) @Max(40)
    private int fontSize = 20;
}

