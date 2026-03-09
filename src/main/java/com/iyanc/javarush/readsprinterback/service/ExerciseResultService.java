package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.request.ExerciseResultRequest;
import com.iyanc.javarush.readsprinterback.dto.response.ExerciseResultResponse;
import com.iyanc.javarush.readsprinterback.dto.response.ExerciseSummaryResponse;

import java.util.List;

public interface ExerciseResultService {
    ExerciseResultResponse save(String userEmail, ExerciseResultRequest request);
    List<ExerciseResultResponse> getMyResults(String userEmail);
    List<ExerciseResultResponse> getMyResultsByType(String userEmail, String exerciseType);
    List<ExerciseSummaryResponse> getMySummary(String userEmail);
}

