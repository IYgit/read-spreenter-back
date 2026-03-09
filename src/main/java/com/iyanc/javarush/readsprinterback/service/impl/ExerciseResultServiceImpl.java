package com.iyanc.javarush.readsprinterback.service.impl;

import com.iyanc.javarush.readsprinterback.dto.request.ExerciseResultRequest;
import com.iyanc.javarush.readsprinterback.dto.response.ExerciseResultResponse;
import com.iyanc.javarush.readsprinterback.dto.response.ExerciseSummaryResponse;
import com.iyanc.javarush.readsprinterback.entity.ExerciseResult;
import com.iyanc.javarush.readsprinterback.entity.Text;
import com.iyanc.javarush.readsprinterback.entity.User;
import com.iyanc.javarush.readsprinterback.exception.ResourceNotFoundException;
import com.iyanc.javarush.readsprinterback.repository.ExerciseResultRepository;
import com.iyanc.javarush.readsprinterback.repository.TextRepository;
import com.iyanc.javarush.readsprinterback.repository.UserRepository;
import com.iyanc.javarush.readsprinterback.service.ExerciseResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExerciseResultServiceImpl implements ExerciseResultService {

    private final ExerciseResultRepository resultRepository;
    private final UserRepository userRepository;
    private final TextRepository textRepository;

    @Override
    @Transactional
    public ExerciseResultResponse save(String userEmail, ExerciseResultRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Text text = null;
        if (request.getTextId() != null) {
            text = textRepository.findById(request.getTextId())
                    .orElseThrow(() -> new ResourceNotFoundException("Text not found with id: " + request.getTextId()));
        }

        ExerciseResult result = ExerciseResult.builder()
                .user(user)
                .exerciseType(request.getExerciseType())
                .text(text)
                .score(request.getScore())
                .durationSec(request.getDurationSec())
                .wpm(request.getWpm())
                .correctCount(request.getCorrectCount())
                .totalCount(request.getTotalCount())
                .extraData(request.getExtraData())
                .build();

        return toResponse(resultRepository.save(result));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseResultResponse> getMyResults(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return resultRepository.findByUserIdOrderByCompletedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseResultResponse> getMyResultsByType(String userEmail, String exerciseType) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return resultRepository.findByUserIdAndExerciseTypeOrderByCompletedAtDesc(user.getId(), exerciseType)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseSummaryResponse> getMySummary(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return resultRepository.findSummaryByUserId(user.getId()).stream()
                .map(row -> ExerciseSummaryResponse.builder()
                        .exerciseType((String) row[0])
                        .totalCount((Long) row[1])
                        .avgScore(row[2] != null ? ((Number) row[2]).doubleValue() : null)
                        .avgWpm(row[3] != null ? ((Number) row[3]).doubleValue() : null)
                        .avgDurationSec(row[4] != null ? ((Number) row[4]).doubleValue() : null)
                        .build())
                .toList();
    }

    private ExerciseResultResponse toResponse(ExerciseResult r) {
        return ExerciseResultResponse.builder()
                .id(r.getId())
                .exerciseType(r.getExerciseType())
                .textId(r.getText() != null ? r.getText().getId() : null)
                .textTitle(r.getText() != null ? r.getText().getTitle() : null)
                .score(r.getScore())
                .durationSec(r.getDurationSec())
                .wpm(r.getWpm())
                .correctCount(r.getCorrectCount())
                .totalCount(r.getTotalCount())
                .extraData(r.getExtraData())
                .completedAt(r.getCompletedAt())
                .build();
    }
}

