package com.iyanc.javarush.readsprinterback.controller;

import com.iyanc.javarush.readsprinterback.dto.request.ExerciseResultRequest;
import com.iyanc.javarush.readsprinterback.dto.response.ExerciseResultResponse;
import com.iyanc.javarush.readsprinterback.dto.response.ExerciseSummaryResponse;
import com.iyanc.javarush.readsprinterback.service.ExerciseResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
@Tag(name = "Exercise Results", description = "Save and retrieve exercise results")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseResultController {

    private final ExerciseResultService resultService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save exercise result for the current user")
    public ExerciseResultResponse saveResult(Authentication auth,
                                             @Valid @RequestBody ExerciseResultRequest request) {
        return resultService.save(auth.getName(), request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get all results for the current user")
    public List<ExerciseResultResponse> getMyResults(Authentication auth) {
        return resultService.getMyResults(auth.getName());
    }

    @GetMapping("/me/summary")
    @Operation(summary = "Get aggregated stats per exercise type for the current user")
    public List<ExerciseSummaryResponse> getMySummary(Authentication auth) {
        return resultService.getMySummary(auth.getName());
    }

    @GetMapping("/me/type/{exerciseType}")
    @Operation(summary = "Get results for a specific exercise type")
    public List<ExerciseResultResponse> getMyResultsByType(Authentication auth,
                                                            @PathVariable String exerciseType) {
        return resultService.getMyResultsByType(auth.getName(), exerciseType);
    }
}

