package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Queue request for the Word Search duel exercise. */
@Data
@EqualsAndHashCode(callSuper = true)
public class WordSearchQueueRequest extends JoinQueueRequest {

    /** Number of rows in the letter grid (8..16). */
    @Min(8) @Max(16)
    private int wsRows = 12;

    /** Number of columns in the letter grid (9..15). */
    @Min(9) @Max(15)
    private int wsCols = 11;

    /** Number of words to find (2..6). */
    @Min(2) @Max(6)
    private int wsWordCount = 3;

    /** Cell font size in px (12..24). */
    @Min(12) @Max(24)
    private int wsFontSize = 16;
}

