package com.iyanc.javarush.readsprinterback.service.impl;

import com.iyanc.javarush.readsprinterback.dto.response.TextResponse;
import com.iyanc.javarush.readsprinterback.entity.Question;
import com.iyanc.javarush.readsprinterback.entity.QuestionOption;
import com.iyanc.javarush.readsprinterback.entity.Text;
import com.iyanc.javarush.readsprinterback.exception.ResourceNotFoundException;
import com.iyanc.javarush.readsprinterback.repository.TextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TextServiceImpl}.
 * All dependencies are replaced with Mockito mocks — no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TextServiceImpl")
class TextServiceImplTest {

    @Mock
    private TextRepository textRepository;

    @InjectMocks
    private TextServiceImpl service;

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    private Text buildText(Long id, String title, String content, Text.Difficulty difficulty,
                           List<Question> questions) {
        return Text.builder()
                .id(id)
                .title(title)
                .content(content)
                .difficulty(difficulty)
                .questions(questions)
                .build();
    }

    private Question buildQuestion(Long id, String questionText, int correctIndex,
                                   List<QuestionOption> options) {
        return Question.builder()
                .id(id)
                .questionText(questionText)
                .correctIndex(correctIndex)
                .options(options)
                .build();
    }

    private QuestionOption buildOption(Long id, String optionText, int sortOrder) {
        return QuestionOption.builder()
                .id(id)
                .optionText(optionText)
                .sortOrder(sortOrder)
                .build();
    }

    private Text text1;
    private Text text2;
    private Text text3;

    @BeforeEach
    void setUp() {
        text1 = buildText(1L, "Title One", "Content one", Text.Difficulty.easy, new ArrayList<>());
        text2 = buildText(2L, "Title Two", "Content two", Text.Difficulty.medium, new ArrayList<>());
        text3 = buildText(3L, "Title Three", "Content three", Text.Difficulty.hard, new ArrayList<>());
    }

    // ─── 4.1 getAllTexts — 3 тексти ────────────────────────────────────────────

    @Test
    @DisplayName("4.1 getAllTexts — репозиторій повертає 3 тексти → список з 3 TextResponse")
    void getAllTexts_returnsThreeResponses() {
        when(textRepository.findAllByOrderByIdAsc()).thenReturn(List.of(text1, text2, text3));

        List<TextResponse> result = service.getAllTexts();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(TextResponse::getId)
                .containsExactly(1L, 2L, 3L);
        assertThat(result).extracting(TextResponse::getTitle)
                .containsExactly("Title One", "Title Two", "Title Three");
        assertThat(result).extracting(TextResponse::getDifficulty)
                .containsExactly("easy", "medium", "hard");
    }

    // ─── 4.2 getAllTexts — порожній список ─────────────────────────────────────

    @Test
    @DisplayName("4.2 getAllTexts — репозиторій повертає порожній список → порожній список")
    void getAllTexts_emptyRepository_returnsEmptyList() {
        when(textRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        List<TextResponse> result = service.getAllTexts();

        assertThat(result).isEmpty();
    }

    // ─── 4.3 getTextById — текст знайдено з питаннями та варіантами ───────────

    @Test
    @DisplayName("4.3 getTextById — текст знайдено → повертає TextResponse з питаннями та варіантами")
    void getTextById_textFound_returnsCorrectlyMappedResponse() {
        QuestionOption opt1 = buildOption(1L, "Option A", 0);
        QuestionOption opt2 = buildOption(2L, "Option B", 1);
        QuestionOption opt3 = buildOption(3L, "Option C", 2);
        Question question = buildQuestion(10L, "What is this about?", 1, List.of(opt1, opt2, opt3));
        Text textWithQuestions = buildText(1L, "Title One", "Content one",
                Text.Difficulty.easy, List.of(question));

        when(textRepository.findById(1L)).thenReturn(Optional.of(textWithQuestions));

        TextResponse response = service.getTextById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Title One");
        assertThat(response.getContent()).isEqualTo("Content one");
        assertThat(response.getDifficulty()).isEqualTo("easy");
        assertThat(response.getQuestions()).hasSize(1);

        var questionResponse = response.getQuestions().get(0);
        assertThat(questionResponse.getId()).isEqualTo(10L);
        assertThat(questionResponse.getText()).isEqualTo("What is this about?");
        assertThat(questionResponse.getCorrectIndex()).isEqualTo(1);
        assertThat(questionResponse.getOptions())
                .containsExactly("Option A", "Option B", "Option C");
    }

    // ─── 4.4 getTextById — текст не знайдено ──────────────────────────────────

    @Test
    @DisplayName("4.4 getTextById — текст не знайдено → кидає ResourceNotFoundException з id у повідомленні")
    void getTextById_textNotFound_throwsResourceNotFoundException() {
        when(textRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTextById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── 4.5 getTextById — текст без питань ───────────────────────────────────

    @Test
    @DisplayName("4.5 getTextById — текст без питань → повертає TextResponse з порожнім списком questions")
    void getTextById_textWithoutQuestions_returnsEmptyQuestionsList() {
        when(textRepository.findById(1L)).thenReturn(Optional.of(text1));

        TextResponse response = service.getTextById(1L);

        assertThat(response.getQuestions()).isEmpty();
    }
}

