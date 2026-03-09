package com.iyanc.javarush.readsprinterback.service.impl;

import com.iyanc.javarush.readsprinterback.dto.response.QuestionResponse;
import com.iyanc.javarush.readsprinterback.dto.response.TextResponse;
import com.iyanc.javarush.readsprinterback.entity.Text;
import com.iyanc.javarush.readsprinterback.exception.ResourceNotFoundException;
import com.iyanc.javarush.readsprinterback.repository.TextRepository;
import com.iyanc.javarush.readsprinterback.service.TextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TextServiceImpl implements TextService {

    private final TextRepository textRepository;

    @Override
    public List<TextResponse> getAllTexts() {
        return textRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TextResponse getTextById(Long id) {
        Text text = textRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Text not found with id: " + id));
        return toResponse(text);
    }

    private TextResponse toResponse(Text text) {
        List<QuestionResponse> questions = text.getQuestions().stream()
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .text(q.getQuestionText())
                        .options(q.getOptions().stream()
                                .map(opt -> opt.getOptionText())
                                .toList())
                        .correctIndex(q.getCorrectIndex())
                        .build())
                .toList();

        return TextResponse.builder()
                .id(text.getId())
                .title(text.getTitle())
                .content(text.getContent())
                .difficulty(text.getDifficulty().name())
                .questions(questions)
                .build();
    }
}

