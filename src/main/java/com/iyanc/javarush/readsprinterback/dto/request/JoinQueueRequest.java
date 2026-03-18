package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinQueueRequest {

    /** "schulte-table" or "numbers" */
    @NotBlank
    private String exerciseType;

    // ── Schulte Table parameters ──────────────────────────────────────────────

    @Min(3) @Max(7)
    private int gridSize = 5;

    @Min(12) @Max(40)
    private int fontSize = 20;

    // ── Numbers exercise parameters ───────────────────────────────────────────

    /** Number of digits per round (3..8) */
    @Min(3) @Max(8)
    private int digitCount = 3;

    /** Number display time in milliseconds (50..2000) */
    @Min(50) @Max(2000)
    private int displayTime = 1000;
}



