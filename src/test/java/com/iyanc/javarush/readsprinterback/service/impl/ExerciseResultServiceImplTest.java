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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExerciseResultServiceImpl}.
 * All dependencies are replaced with Mockito mocks — no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExerciseResultServiceImpl")
class ExerciseResultServiceImplTest {

    @Mock private ExerciseResultRepository resultRepository;
    @Mock private UserRepository           userRepository;
    @Mock private TextRepository           textRepository;

    @InjectMocks
    private ExerciseResultServiceImpl service;

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    private static final String EMAIL         = "user@example.com";
    private static final Long   USER_ID       = 10L;
    private static final Long   TEXT_ID       = 5L;
    private static final String TEXT_TITLE    = "Sample Text Title";
    private static final String EXERCISE_TYPE = "rsvp";

    private User          user;
    private Text          text;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        user = User.builder()
                .id(USER_ID)
                .username("testuser")
                .email(EMAIL)
                .passwordHash("hash")
                .role(User.Role.USER)
                .build();

        text = Text.builder()
                .id(TEXT_ID)
                .title(TEXT_TITLE)
                .content("Some content")
                .difficulty(Text.Difficulty.easy)
                .build();
    }

    // ─── save ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("3.1 save — запит без textId → text=null, textRepository не викликається, повертає ExerciseResultResponse")
    void save_withoutTextId_savesResultWithNullText() {
        ExerciseResultRequest req = buildRequest(null, 850, 60, 300, 8, 10);
        ExerciseResult saved = buildResult(null, 100L, 850, 60, 300, 8, 10, now);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.save(any(ExerciseResult.class))).thenReturn(saved);

        ExerciseResultResponse response = service.save(EMAIL, req);

        assertThat(response).isNotNull();
        assertThat(response.getExerciseType()).isEqualTo(EXERCISE_TYPE);
        assertThat(response.getTextId()).isNull();
        assertThat(response.getTextTitle()).isNull();
        assertThat(response.getScore()).isEqualTo(850);
        assertThat(response.getDurationSec()).isEqualTo(60);
        assertThat(response.getCompletedAt()).isEqualTo(now);

        verify(textRepository, never()).findById(any());
    }

    @Test
    @DisplayName("3.2 save — запит з валідним textId → findById() викликається; textTitle є у відповіді")
    void save_withValidTextId_callsFindByIdAndIncludesTitle() {
        ExerciseResultRequest req = buildRequest(TEXT_ID, 900, 45, null, null, null);
        ExerciseResult saved = buildResult(text, 101L, 900, 45, null, null, null, now);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(textRepository.findById(TEXT_ID)).thenReturn(Optional.of(text));
        when(resultRepository.save(any(ExerciseResult.class))).thenReturn(saved);

        ExerciseResultResponse response = service.save(EMAIL, req);

        verify(textRepository).findById(TEXT_ID);
        assertThat(response.getTextId()).isEqualTo(TEXT_ID);
        assertThat(response.getTextTitle()).isEqualTo(TEXT_TITLE);
    }

    @Test
    @DisplayName("3.3 save — textId вказано, але текст не знайдено → ResourceNotFoundException з id у повідомленні")
    void save_textNotFound_throwsResourceNotFoundException() {
        ExerciseResultRequest req = buildRequest(TEXT_ID, null, null, null, null, null);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(textRepository.findById(TEXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(EMAIL, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(TEXT_ID));

        verify(resultRepository, never()).save(any());
    }

    @Test
    @DisplayName("3.4 save — користувач не знайдений → ResourceNotFoundException; textRepository та save не викликаються")
    void save_userNotFound_throwsResourceNotFoundException() {
        ExerciseResultRequest req = buildRequest(null, 500, 30, 200, 5, 10);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(EMAIL, req))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(textRepository, never()).findById(any());
        verify(resultRepository, never()).save(any());
    }

    // ─── getMyResults ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("3.5 getMyResults — є результати → повертає список, перший елемент = найновіший (DESC)")
    void getMyResults_hasResults_returnsListInDescOrder() {
        ExerciseResult older  = buildResult(null, 1L, 700, 50, null, null, null, now.minusMinutes(5));
        ExerciseResult newer  = buildResult(null, 2L, 800, 40, null, null, null, now);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findByUserIdOrderByCompletedAtDesc(USER_ID))
                .thenReturn(List.of(newer, older));   // DESC — newer first

        List<ExerciseResultResponse> result = service.getMyResults(EMAIL);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getScore()).isEqualTo(800);   // newer
        assertThat(result.get(1).getScore()).isEqualTo(700);   // older
    }

    @Test
    @DisplayName("3.6 getMyResults — результатів немає → повертає порожній список")
    void getMyResults_noResults_returnsEmptyList() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findByUserIdOrderByCompletedAtDesc(USER_ID)).thenReturn(List.of());

        assertThat(service.getMyResults(EMAIL)).isEmpty();
    }

    // ─── getMyResultsByType ───────────────────────────────────────────────────

    @Test
    @DisplayName("3.7 getMyResultsByType — є результати відповідного типу → повертає відфільтрований список")
    void getMyResultsByType_returnsMatchingType() {
        ExerciseResult r = buildResult(null, 3L, 600, 30, null, null, null, now);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findByUserIdAndExerciseTypeOrderByCompletedAtDesc(USER_ID, EXERCISE_TYPE))
                .thenReturn(List.of(r));

        List<ExerciseResultResponse> result = service.getMyResultsByType(EMAIL, EXERCISE_TYPE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExerciseType()).isEqualTo(EXERCISE_TYPE);
    }

    @Test
    @DisplayName("3.7b getMyResultsByType — результатів немає для типу → порожній список")
    void getMyResultsByType_noMatchingType_returnsEmptyList() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findByUserIdAndExerciseTypeOrderByCompletedAtDesc(USER_ID, "numbers"))
                .thenReturn(List.of());

        assertThat(service.getMyResultsByType(EMAIL, "numbers")).isEmpty();
    }

    // ─── getMySummary ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("3.8 getMySummary — є зведені дані → коректно маппить Object[] на ExerciseSummaryResponse")
    void getMySummary_hasData_mapsRowsCorrectly() {
        Object[] row = new Object[]{"rsvp", 5L, 750.0, 280.0, 45.0};

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findSummaryByUserId(USER_ID)).thenReturn(List.<Object[]>of(row));

        List<ExerciseSummaryResponse> summary = service.getMySummary(EMAIL);

        assertThat(summary).hasSize(1);
        ExerciseSummaryResponse s = summary.get(0);
        assertThat(s.getExerciseType()).isEqualTo("rsvp");
        assertThat(s.getTotalCount()).isEqualTo(5L);
        assertThat(s.getAvgScore()).isEqualTo(750.0);
        assertThat(s.getAvgWpm()).isEqualTo(280.0);
        assertThat(s.getAvgDurationSec()).isEqualTo(45.0);
    }

    @Test
    @DisplayName("3.9 getMySummary — avgScore/avgWpm/avgDurationSec = null → поля у відповіді null, без NPE")
    void getMySummary_nullAggregates_fieldsAreNull() {
        Object[] row = new Object[]{"numbers", 3L, null, null, null};

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findSummaryByUserId(USER_ID)).thenReturn(List.<Object[]>of(row));

        List<ExerciseSummaryResponse> summary = service.getMySummary(EMAIL);

        assertThat(summary).hasSize(1);
        ExerciseSummaryResponse s = summary.get(0);
        assertThat(s.getExerciseType()).isEqualTo("numbers");
        assertThat(s.getTotalCount()).isEqualTo(3L);
        assertThat(s.getAvgScore()).isNull();
        assertThat(s.getAvgWpm()).isNull();
        assertThat(s.getAvgDurationSec()).isNull();
    }

    @Test
    @DisplayName("3.9b getMySummary — avgScore як Integer → коректно конвертується у Double через Number.doubleValue()")
    void getMySummary_integerAggregates_convertedToDouble() {
        // PostgreSQL AVG may return BigDecimal or Integer depending on context
        Object[] row = new Object[]{"schulte-table", 2L, 920, 0, 30};

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findSummaryByUserId(USER_ID)).thenReturn(List.<Object[]>of(row));

        List<ExerciseSummaryResponse> summary = service.getMySummary(EMAIL);

        assertThat(summary.get(0).getAvgScore()).isEqualTo(920.0);
        assertThat(summary.get(0).getAvgWpm()).isEqualTo(0.0);
        assertThat(summary.get(0).getAvgDurationSec()).isEqualTo(30.0);
    }

    // ─── save: full response mapping / ArgumentCaptor ─────────────────────────

    @Test
    @DisplayName("3.10 save — усі поля відповіді коректно замапіровані (id, wpm, correctCount, totalCount, extraData)")
    void save_allResponseFieldsMapped() {
        Map<String, Object> extra = Map.of("speed", 3);
        ExerciseResultRequest req = buildRequest(null, 700, 55, 250, 7, 10);
        req.setExtraData(extra);

        ExerciseResult saved = ExerciseResult.builder()
                .id(42L)
                .user(user)
                .exerciseType(EXERCISE_TYPE)
                .text(null)
                .score(700)
                .durationSec(55)
                .wpm(250)
                .correctCount(7)
                .totalCount(10)
                .extraData(extra)
                .completedAt(now)
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.save(any(ExerciseResult.class))).thenReturn(saved);

        ExerciseResultResponse resp = service.save(EMAIL, req);

        assertThat(resp.getId()).isEqualTo(42L);
        assertThat(resp.getWpm()).isEqualTo(250);
        assertThat(resp.getCorrectCount()).isEqualTo(7);
        assertThat(resp.getTotalCount()).isEqualTo(10);
        assertThat(resp.getExtraData()).isEqualTo(extra);
        assertThat(resp.getDurationSec()).isEqualTo(55);
    }

    @Test
    @DisplayName("3.11 save — entity, передана в resultRepository.save(), містить правильні поля (ArgumentCaptor)")
    void save_capturedEntityHasCorrectFields() {
        Map<String, Object> extra = Map.of("mode", "fast");
        ExerciseResultRequest req = buildRequest(TEXT_ID, 800, 40, 300, 9, 12);
        req.setExtraData(extra);

        ExerciseResult saved = buildResult(text, 200L, 800, 40, 300, 9, 12, now);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(textRepository.findById(TEXT_ID)).thenReturn(Optional.of(text));
        when(resultRepository.save(any(ExerciseResult.class))).thenReturn(saved);

        service.save(EMAIL, req);

        ArgumentCaptor<ExerciseResult> captor = ArgumentCaptor.forClass(ExerciseResult.class);
        verify(resultRepository).save(captor.capture());

        ExerciseResult captured = captor.getValue();
        assertThat(captured.getUser()).isEqualTo(user);
        assertThat(captured.getText()).isEqualTo(text);
        assertThat(captured.getExerciseType()).isEqualTo(EXERCISE_TYPE);
        assertThat(captured.getScore()).isEqualTo(800);
        assertThat(captured.getDurationSec()).isEqualTo(40);
        assertThat(captured.getWpm()).isEqualTo(300);
        assertThat(captured.getCorrectCount()).isEqualTo(9);
        assertThat(captured.getTotalCount()).isEqualTo(12);
        assertThat(captured.getExtraData()).isEqualTo(extra);
    }

    // ─── getMyResults: user not found ─────────────────────────────────────────

    @Test
    @DisplayName("3.12 getMyResults — користувач не знайдений → ResourceNotFoundException; репозиторій не викликається")
    void getMyResults_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyResults(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(resultRepository, never()).findByUserIdOrderByCompletedAtDesc(any());
    }

    // ─── getMyResultsByType: edge cases ───────────────────────────────────────

    @Test
    @DisplayName("3.13 getMyResultsByType — користувач не знайдений → ResourceNotFoundException; репозиторій не викликається")
    void getMyResultsByType_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyResultsByType(EMAIL, EXERCISE_TYPE))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(resultRepository, never())
                .findByUserIdAndExerciseTypeOrderByCompletedAtDesc(any(), any());
    }

    @Test
    @DisplayName("3.14 getMyResultsByType — результат прив'язаний до тексту → textId і textTitle присутні у відповіді")
    void getMyResultsByType_resultWithText_hasTitleAndId() {
        ExerciseResult r = buildResult(text, 5L, 650, 35, 200, 6, 8, now);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findByUserIdAndExerciseTypeOrderByCompletedAtDesc(USER_ID, EXERCISE_TYPE))
                .thenReturn(List.of(r));

        List<ExerciseResultResponse> result = service.getMyResultsByType(EMAIL, EXERCISE_TYPE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTextId()).isEqualTo(TEXT_ID);
        assertThat(result.get(0).getTextTitle()).isEqualTo(TEXT_TITLE);
    }

    // ─── getMySummary: missing branches ───────────────────────────────────────

    @Test
    @DisplayName("3.15 getMySummary — користувач не знайдений → ResourceNotFoundException; репозиторій не викликається")
    void getMySummary_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMySummary(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(resultRepository, never()).findSummaryByUserId(any());
    }

    @Test
    @DisplayName("3.16 getMySummary — результатів немає → повертає порожній список")
    void getMySummary_noResults_returnsEmptyList() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findSummaryByUserId(USER_ID)).thenReturn(List.of());

        assertThat(service.getMySummary(EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("3.17 getMySummary — BigDecimal агрегати (PostgreSQL AVG) → коректно конвертуються у Double")
    void getMySummary_bigDecimalAggregates_convertedToDouble() {
        Object[] row = new Object[]{
                "rsvp", 4L,
                new BigDecimal("875.50"),
                new BigDecimal("310.25"),
                new BigDecimal("42.00")
        };

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findSummaryByUserId(USER_ID)).thenReturn(List.<Object[]>of(row));

        List<ExerciseSummaryResponse> summary = service.getMySummary(EMAIL);

        assertThat(summary).hasSize(1);
        ExerciseSummaryResponse s = summary.get(0);
        assertThat(s.getAvgScore()).isEqualTo(875.5);
        assertThat(s.getAvgWpm()).isEqualTo(310.25);
        assertThat(s.getAvgDurationSec()).isEqualTo(42.0);
    }

    @Test
    @DisplayName("3.18 getMySummary — декілька рядків → всі рядки замапіровані у відповідний порядок")
    void getMySummary_multipleRows_allRowsMapped() {
        Object[] row1 = new Object[]{"rsvp",    3L, 800.0, 300.0, 50.0};
        Object[] row2 = new Object[]{"numbers", 2L, 600.0,   0.0, 25.0};

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(resultRepository.findSummaryByUserId(USER_ID)).thenReturn(List.<Object[]>of(row1, row2));

        List<ExerciseSummaryResponse> summary = service.getMySummary(EMAIL);

        assertThat(summary).hasSize(2);
        assertThat(summary.get(0).getExerciseType()).isEqualTo("rsvp");
        assertThat(summary.get(0).getTotalCount()).isEqualTo(3L);
        assertThat(summary.get(1).getExerciseType()).isEqualTo("numbers");
        assertThat(summary.get(1).getTotalCount()).isEqualTo(2L);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private ExerciseResultRequest buildRequest(Long textId, Integer score,
                                               Integer durationSec, Integer wpm,
                                               Integer correctCount, Integer totalCount) {
        ExerciseResultRequest req = new ExerciseResultRequest();
        req.setExerciseType(EXERCISE_TYPE);
        req.setTextId(textId);
        req.setScore(score);
        req.setDurationSec(durationSec);
        req.setWpm(wpm);
        req.setCorrectCount(correctCount);
        req.setTotalCount(totalCount);
        return req;
    }

    private ExerciseResult buildResult(Text t, Long id, Integer score,
                                       Integer durationSec, Integer wpm,
                                       Integer correctCount, Integer totalCount,
                                       LocalDateTime completedAt) {
        return ExerciseResult.builder()
                .id(id)
                .user(user)
                .exerciseType(EXERCISE_TYPE)
                .text(t)
                .score(score)
                .durationSec(durationSec)
                .wpm(wpm)
                .correctCount(correctCount)
                .totalCount(totalCount)
                .completedAt(completedAt)
                .build();
    }
}

