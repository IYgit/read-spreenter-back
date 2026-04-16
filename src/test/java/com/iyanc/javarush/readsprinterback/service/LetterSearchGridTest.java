package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MatchmakingDbService#generateLetterSearchGrid} (cases 6.1–6.4).
 * Method and result record are private — accessed via reflection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchmakingDbService — generateLetterSearchGrid")
class LetterSearchGridTest {

    @Mock private MatchmakingQueueRepository   queueRepository;
    @Mock private DuelSessionRepository        sessionRepository;
    @Mock private DuelParticipantRepository    participantRepository;
    @Mock private UserRepository               userRepository;
    @Mock private WpDiffPairRepository         wpDiffPairRepository;
    @Mock private WpSameWordRepository         wpSameWordRepository;
    @Mock private WsWordBankRepository         wsWordBankRepository;
    @Mock private TextRepository               textRepository;

    @InjectMocks
    private MatchmakingDbService service;

    private static final int ROWS         = 10;
    private static final int COLS         = 8;
    private static final int LETTER_COUNT = 2;

    /**
     * Invokes the private {@code generateLetterSearchGrid} and returns the raw result object.
     * Fields are read via reflection because {@code LetterSearchGridData} is a private record.
     */
    private Object invokeGenerate(int rows, int cols, int letterCount) throws Exception {
        Method m = MatchmakingDbService.class
                .getDeclaredMethod("generateLetterSearchGrid", int.class, int.class, int.class);
        m.setAccessible(true);
        return m.invoke(service, rows, cols, letterCount);
    }

    @SuppressWarnings("unchecked")
    private List<String> getTargetLetters(Object gridData) throws Exception {
        return (List<String>) gridData.getClass().getMethod("targetLetters").invoke(gridData);
    }

    private String[][] getGrid(Object gridData) throws Exception {
        return (String[][]) gridData.getClass().getMethod("grid").invoke(gridData);
    }

    private int getTotalTargets(Object gridData) throws Exception {
        return (int) gridData.getClass().getMethod("totalTargets").invoke(gridData);
    }

    // ─── 6.1 розмір сітки 10×8 ───────────────────────────────────────────────

    @RepeatedTest(3)
    @DisplayName("6.1 generateLetterSearchGrid — rows=10, cols=8 → сітка має розмір 10×8")
    void grid_hasCorrectDimensions() throws Exception {
        Object result = invokeGenerate(ROWS, COLS, LETTER_COUNT);
        String[][] grid = getGrid(result);

        assertThat(grid.length).isEqualTo(ROWS);
        for (String[] row : grid) {
            assertThat(row.length).isEqualTo(COLS);
        }
    }

    // ─── 6.2 кожна цільова літера зустрічається ≥ 2 рази ─────────────────────

    @RepeatedTest(3)
    @DisplayName("6.2 generateLetterSearchGrid — кожна цільова літера зустрічається ≥ 2 рази")
    void grid_eachTargetLetterAppearsAtLeastTwice() throws Exception {
        Object result = invokeGenerate(ROWS, COLS, LETTER_COUNT);
        String[][] grid = getGrid(result);
        List<String> targets = getTargetLetters(result);

        for (String target : targets) {
            long count = 0;
            for (String[] row : grid) {
                for (String cell : row) {
                    if (target.equals(cell)) count++;
                }
            }
            assertThat(count)
                    .as("Target letter '%s' should appear at least 2 times in the grid", target)
                    .isGreaterThanOrEqualTo(2);
        }
    }

    // ─── 6.3 нецільові клітинки не містять цільових літер ────────────────────

    @RepeatedTest(3)
    @DisplayName("6.3 generateLetterSearchGrid — totalTargets = кількість цільових клітинок у сітці")
    void grid_totalTargetsMatchesActualTargetCellCount() throws Exception {
        Object result = invokeGenerate(ROWS, COLS, LETTER_COUNT);
        String[][] grid = getGrid(result);
        List<String> targets = getTargetLetters(result);
        int reportedTotal = getTotalTargets(result);

        long actualCount = 0;
        for (String[] row : grid) {
            for (String cell : row) {
                if (targets.contains(cell)) actualCount++;
            }
        }
        assertThat(actualCount)
                .as("Actual target cell count in grid should equal reportedTotal")
                .isEqualTo(reportedTotal);
    }

    // ─── 6.4 кількість унікальних цільових літер = letterCount ───────────────

    @RepeatedTest(3)
    @DisplayName("6.4 generateLetterSearchGrid — кількість унікальних цільових літер = letterCount")
    void grid_uniqueTargetLetterCountEqualsLetterCount() throws Exception {
        Object result = invokeGenerate(ROWS, COLS, LETTER_COUNT);
        List<String> targets = getTargetLetters(result);

        Set<String> unique = new HashSet<>(targets);
        assertThat(unique).hasSize(LETTER_COUNT);
    }
}

