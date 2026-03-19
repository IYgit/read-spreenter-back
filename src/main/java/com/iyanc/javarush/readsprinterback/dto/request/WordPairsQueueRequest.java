package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WordPairsQueueRequest extends JoinQueueRequest {

    /** Grid rows (3..5) */
    @Min(3) @Max(5)
    private int wpRows = 4;

    /** Grid cols (3..5) */
    @Min(3) @Max(5)
    private int wpCols = 4;

    /** Time limit in seconds (30..120) */
    @Min(30) @Max(120)
    private int wpTimeLimit = 60;

    /** Font size in px (12..18) */
    @Min(12) @Max(18)
    private int wpFontSize = 14;
}

