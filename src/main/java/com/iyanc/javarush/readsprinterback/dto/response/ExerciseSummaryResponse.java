package com.iyanc.javarush.readsprinterback.dto.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExerciseSummaryResponse {
    private String exerciseType;
    private Long totalCount;
    private Double avgScore;
    private Double avgWpm;
    private Double avgDurationSec;
}

