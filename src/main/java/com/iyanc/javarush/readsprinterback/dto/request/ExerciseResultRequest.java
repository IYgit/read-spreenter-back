package com.iyanc.javarush.readsprinterback.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;

@Data
public class ExerciseResultRequest {

    @NotBlank
    @Size(max = 50)
    private String exerciseType;

    private Long textId;

    @Min(0)
    private Integer score;

    @Min(0)
    private Integer durationSec;

    @Min(0)
    private Integer wpm;

    @Min(0)
    private Integer correctCount;

    @Min(0)
    private Integer totalCount;

    private Map<String, Object> extraData;
}

