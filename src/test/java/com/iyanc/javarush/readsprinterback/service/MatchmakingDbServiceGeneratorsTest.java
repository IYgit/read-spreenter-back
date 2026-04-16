package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for pure generator methods in {@link MatchmakingDbService}.
 * No Spring context — all dependencies mocked, methods accessed via reflection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchmakingDbService — генератори даних")
class MatchmakingDbServiceGeneratorsTest {

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

    // ── Reflection helpers ────────────────────────────────────────────────────

    private int[] invokeGenerateRandomNumbers(int digitCount) throws Exception {
        Method m = MatchmakingDbService.class.getDeclaredMethod("generateRandomNumbers", int.class);
        m.setAccessible(true);
        return (int[]) m.invoke(service, digitCount);
    }

    private int[] invokeGenerateShuffledNumbers(int count) throws Exception {
        Method m = MatchmakingDbService.class.getDeclaredMethod("generateShuffledNumbers", int.class);
        m.setAccessible(true);
        return (int[]) m.invoke(service, count);
    }

    private boolean invokeIsValidDiffPair(String w1, String w2) throws Exception {
        Method m = MatchmakingDbService.class.getDeclaredMethod("isValidDiffPair", String.class, String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, w1, w2);
    }

    private boolean invokeContainsLatinHomoglyph(String word) throws Exception {
        Method m = MatchmakingDbService.class.getDeclaredMethod("containsLatinHomoglyph", String.class);
        m.setAccessible(true);
        return (boolean) m.invoke(null, word);
    }

    private int invokeSafeInt(Integer lsRows, Integer lsCols) throws Exception {
        Method m = MatchmakingDbService.class.getDeclaredMethod("safeInt", Integer.class, Integer.class);
        m.setAccessible(true);
        return (int) m.invoke(service, lsRows, lsCols);
    }

    private int invokeSafeIntLs(Integer val) throws Exception {
        Method m = MatchmakingDbService.class.getDeclaredMethod("safeIntLs", Integer.class);
        m.setAccessible(true);
        return (int) m.invoke(service, val);
    }

    // ─── 5.1 generateRandomNumbers — digitCount=1 ────────────────────────────

    @Test
    @DisplayName("5.1 generateRandomNumbers — digitCount=1 → всі числа в діапазоні [1, 9]")
    void generateRandomNumbers_digitCount1_allInRange1to9() throws Exception {
        int[] numbers = invokeGenerateRandomNumbers(1);

        for (int n : numbers) {
            assertThat(n).isBetween(1, 9);
        }
    }

    // ─── 5.2 generateRandomNumbers — digitCount=3 ────────────────────────────

    @Test
    @DisplayName("5.2 generateRandomNumbers — digitCount=3 → всі числа в діапазоні [100, 999]")
    void generateRandomNumbers_digitCount3_allInRange100to999() throws Exception {
        int[] numbers = invokeGenerateRandomNumbers(3);

        for (int n : numbers) {
            assertThat(n).isBetween(100, 999);
        }
    }

    // ─── 5.3 generateRandomNumbers — масив має рівно 10 елементів ─────────────

    @Test
    @DisplayName("5.3 generateRandomNumbers — будь-який digitCount → масив має рівно 10 елементів")
    void generateRandomNumbers_alwaysReturns10Elements() throws Exception {
        assertThat(invokeGenerateRandomNumbers(1)).hasSize(10);
        assertThat(invokeGenerateRandomNumbers(2)).hasSize(10);
        assertThat(invokeGenerateRandomNumbers(4)).hasSize(10);
    }

    // ─── 5.4 generateShuffledNumbers — count=9 ────────────────────────────────

    @RepeatedTest(5)
    @DisplayName("5.4 generateShuffledNumbers — count=9 → числа 1..9; хоч один не на своєму місці")
    void generateShuffledNumbers_count9_contains1to9() throws Exception {
        int[] numbers = invokeGenerateShuffledNumbers(9);

        assertThat(numbers).hasSize(9);
        // Must contain every number 1..9 exactly once
        int[] sorted = numbers.clone();
        java.util.Arrays.sort(sorted);
        assertThat(sorted).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    // ─── 5.5 isValidDiffPair — w1.equals(w2) → false ─────────────────────────

    @Test
    @DisplayName("5.5 isValidDiffPair — однакові слова → false")
    void isValidDiffPair_equalWords_returnsFalse() throws Exception {
        assertThat(invokeIsValidDiffPair("слово", "слово")).isFalse();
    }

    // ─── 5.6 isValidDiffPair — латинська літера в слові → false ──────────────

    @Test
    @DisplayName("5.6 isValidDiffPair — одне слово містить латинську літеру → false")
    void isValidDiffPair_wordWithLatinLetter_returnsFalse() throws Exception {
        // 'o' is Latin homoglyph inside Cyrillic word
        assertThat(invokeIsValidDiffPair("слoво", "книга")).isFalse();
    }

    // ─── 5.7 isValidDiffPair — валідна кирилиця, різні слова → true ──────────

    @Test
    @DisplayName("5.7 isValidDiffPair — обидва слова кирилиця, різні → true")
    void isValidDiffPair_validCyrillicDifferentWords_returnsTrue() throws Exception {
        assertThat(invokeIsValidDiffPair("слово", "книга")).isTrue();
    }

    // ─── 5.8 containsLatinHomoglyph — лише кирилиця → false ─────────────────

    @Test
    @DisplayName("5.8 containsLatinHomoglyph — \"слово\" (лише кирилиця) → false")
    void containsLatinHomoglyph_pureUkrainian_returnsFalse() throws Exception {
        assertThat(invokeContainsLatinHomoglyph("слово")).isFalse();
    }

    // ─── 5.9 containsLatinHomoglyph — латинська 'o' серед кирилиці → true ────

    @Test
    @DisplayName("5.9 containsLatinHomoglyph — \"слoво\" (латинська 'o') → true")
    void containsLatinHomoglyph_latinOInsideCyrillic_returnsTrue() throws Exception {
        // "слoво" — third character is Latin lowercase 'o'
        assertThat(invokeContainsLatinHomoglyph("сл\u006Fво")).isTrue();
    }

    // ─── 5.10 safeInt — lsRows=10, lsCols=8 → index 0 ───────────────────────

    @Test
    @DisplayName("5.10 safeInt — lsRows=10, lsCols=8 → повертає 0 (перший індекс)")
    void safeInt_rows10cols8_returnsIndex0() throws Exception {
        assertThat(invokeSafeInt(10, 8)).isEqualTo(0);
    }

    // ─── 5.11 safeInt — lsRows=null → default 1 ─────────────────────────────

    @Test
    @DisplayName("5.11 safeInt — lsRows=null → повертає 1 (default)")
    void safeInt_nullRows_returnsDefault1() throws Exception {
        assertThat(invokeSafeInt(null, 8)).isEqualTo(1);
    }

    // ─── 5.12 safeIntLs — null → default 2 ──────────────────────────────────

    @Test
    @DisplayName("5.12 safeIntLs — null → повертає 2 (default letterCount)")
    void safeIntLs_null_returnsDefault2() throws Exception {
        assertThat(invokeSafeIntLs(null)).isEqualTo(2);
    }
}

