package com.iyanc.javarush.readsprinterback.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExerciseResultResponse {
    private Long id;
    private String exerciseType;
    private Long textId;
    private String textTitle;
    private Integer score;
    private Integer durationSec;
    private Integer wpm;
    private Integer correctCount;
    private Integer totalCount;
    private Map<String, Object> extraData;
    private LocalDateTime completedAt;
}

