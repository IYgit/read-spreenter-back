package com.iyanc.javarush.readsprinterback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyanc.javarush.readsprinterback.dto.WordPairDto;
import com.iyanc.javarush.readsprinterback.dto.request.JoinQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.request.SchulteQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.request.NumbersQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.request.WordPairsQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RsvpQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.request.WordSearchQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.request.SyntagmQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.response.MatchFoundMessage;
import com.iyanc.javarush.readsprinterback.dto.response.WsWordPosition;
import com.iyanc.javarush.readsprinterback.dto.response.QuestionResponse;
import com.iyanc.javarush.readsprinterback.entity.*;
import com.iyanc.javarush.readsprinterback.exception.ResourceNotFoundException;
import com.iyanc.javarush.readsprinterback.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UncheckedIOException;
import java.util.*;
import java.util.Arrays;

/**
 * Handles all DB operations for matchmaking in a dedicated @Transactional boundary.
 * Separated from MatchmakingService so that Spring AOP proxy intercepts the transaction
 * correctly — self-invocation within the same bean would bypass the proxy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingDbService {

    private static final int NUMBERS_TOTAL_ROUNDS = 10;
    private static final Random RANDOM = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MATCH_FOUND    = "MATCH_FOUND";
    private static final String NUMBERS        = "numbers";
    private static final String SCHULTE_TABLE  = "schulte-table";
    private static final String WORD_PAIRS     = "word-pairs";
    private static final String WORD_SEARCH    = "word-search";
    private static final String SYNTAGM        = "syntagm-reading";

    private static final String UKRAINIAN_LETTERS = "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя";

    private final MatchmakingQueueRepository queueRepository;
    private final DuelSessionRepository sessionRepository;
    private final DuelParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final WpDiffPairRepository wpDiffPairRepository;
    private final WpSameWordRepository wpSameWordRepository;
    private final WsWordBankRepository wsWordBankRepository;
    private final TextRepository textRepository;


    /**
     * All DB operations in a single transaction.
     * Returns MatchResult if a match was created, null if added to waiting queue.
     */
    @Transactional
    public MatchmakingService.MatchResult tryCreateMatch(String username, JoinQueueRequest req) {
        User user = findAndCleanupUser(username);
        Optional<MatchmakingQueue> opponentOpt = findOpponent(user.getId(), req.getExerciseType());

        if (opponentOpt.isEmpty()) {
            addToQueue(user, req);
            return null;
        }

        MatchmakingQueue opponent = opponentOpt.get();
        queueRepository.delete(opponent);

        MatchData data = buildMatch(user, opponent, req);

        return new MatchmakingService.MatchResult(
                data.session().getId(),
                user.getEmail(),
                opponent.getUser().getEmail(),
                data.msgForUser(),
                data.msgForOpponent()
        );
    }

    // ── Dispatcher ───────────────────────────────────────────────────────────

    private MatchData buildMatch(User user, MatchmakingQueue opponent, JoinQueueRequest req) {
        if (req instanceof NumbersQueueRequest nr) {
            return buildNumbersMatch(user, opponent, nr);
        } else if (req instanceof WordPairsQueueRequest wp) {
            return buildWordPairsMatch(user, opponent, wp);
        } else if (req instanceof RsvpQueueRequest rr) {
            return buildRsvpMatch(user, opponent, rr);
        } else if (req instanceof WordSearchQueueRequest ws) {
            return buildWordSearchMatch(user, opponent, ws);
        } else if (req instanceof SyntagmQueueRequest sr) {
            return buildSyntagmMatch(user, opponent, sr);
        } else {
            SchulteQueueRequest sr = req instanceof SchulteQueueRequest s ? s : new SchulteQueueRequest();
            return buildSchulteMatch(user, opponent, sr);
        }
    }

    // ── Per-exercise match builders ───────────────────────────────────────────

    private MatchData buildNumbersMatch(User user, MatchmakingQueue opponent, NumbersQueueRequest req) {
        int finalDigitCount  = Math.min(req.getDigitCount(),  opponent.getDigitCount());
        int finalDisplayTime = Math.max(req.getDisplayTime(), opponent.getDisplayTime());
        int[] numbers = generateRandomNumbers(finalDigitCount);

        DuelSessionParams params = DuelSessionParams.builder()
                .totalCells(NUMBERS_TOTAL_ROUNDS)
                .digitCount(finalDigitCount)
                .displayTimeMs(finalDisplayTime)
                .numbersSequenceJson(toJson(numbers))
                .build();
        DuelSession session = saveSessionWithParticipants(NUMBERS, params, user, opponent.getUser());

        MatchFoundMessage msgForUser = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(opponent.getUser().getUsername()).exerciseType(NUMBERS)
                .digitCount(finalDigitCount).displayTime(finalDisplayTime)
                .totalRounds(NUMBERS_TOTAL_ROUNDS).numbers(numbers).totalCells(NUMBERS_TOTAL_ROUNDS)
                .build();
        MatchFoundMessage msgForOpponent = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(user.getUsername()).exerciseType(NUMBERS)
                .digitCount(finalDigitCount).displayTime(finalDisplayTime)
                .totalRounds(NUMBERS_TOTAL_ROUNDS).numbers(numbers).totalCells(NUMBERS_TOTAL_ROUNDS)
                .build();

        return new MatchData(session, msgForUser, msgForOpponent);
    }

    private MatchData buildWordPairsMatch(User user, MatchmakingQueue opponent, WordPairsQueueRequest req) {
        int finalRows      = Math.min(req.getWpRows(),      opponent.getWpRows());
        int finalCols      = Math.min(req.getWpCols(),      opponent.getWpCols());
        int finalTimeLimit = Math.max(req.getWpTimeLimit(), opponent.getWpTimeLimit());
        int finalFontSize  = Math.max(req.getWpFontSize(),  opponent.getWpFontSize());

        List<WordPairDto> pairsList = generateWordPairs(finalRows * finalCols);
        int diffCount = (int) pairsList.stream().filter(WordPairDto::isDiff).count();
        WordPairDto[] pairs = pairsList.toArray(new WordPairDto[0]);

        DuelSessionParams params = DuelSessionParams.builder()
                .totalCells(diffCount)
                .wpRows(finalRows).wpCols(finalCols)
                .wpTimeLimitSec(finalTimeLimit).wpFontSize(finalFontSize)
                .wordPairsJson(toJsonWordPairs(pairs))
                .build();
        DuelSession session = saveSessionWithParticipants(WORD_PAIRS, params, user, opponent.getUser());

        MatchFoundMessage msgForUser = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(opponent.getUser().getUsername()).exerciseType(WORD_PAIRS)
                .pairs(pairs).wpRows(finalRows).wpCols(finalCols)
                .wpTimeLimit(finalTimeLimit).wpFontSize(finalFontSize).totalCells(diffCount)
                .build();
        MatchFoundMessage msgForOpponent = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(user.getUsername()).exerciseType(WORD_PAIRS)
                .pairs(pairs).wpRows(finalRows).wpCols(finalCols)
                .wpTimeLimit(finalTimeLimit).wpFontSize(finalFontSize).totalCells(diffCount)
                .build();

        return new MatchData(session, msgForUser, msgForOpponent);
    }

    private MatchData buildRsvpMatch(User user, MatchmakingQueue opponent, RsvpQueueRequest req) {
        int finalSyntagmWidth = Math.min(req.getRsvpSyntagmWidth(), opponent.getRsvpSyntagmWidth());
        int finalDisplayTime  = Math.max(req.getRsvpDisplayTime(),  opponent.getRsvpDisplayTime());

        Text text = pickRandomText();
        List<QuestionResponse> questions = buildQuestionResponses(text);

        DuelSessionParams params = DuelSessionParams.builder()
                .totalCells(questions.size())
                .rsvpTextId(text.getId())
                .rsvpSyntagmWidth(finalSyntagmWidth)
                .rsvpDisplayTimeMs(finalDisplayTime)
                .build();
        DuelSession session = saveSessionWithParticipants("rsvp", params, user, opponent.getUser());

        MatchFoundMessage msgForUser = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(opponent.getUser().getUsername()).exerciseType("rsvp")
                .rsvpSyntagmWidth(finalSyntagmWidth).rsvpDisplayTime(finalDisplayTime)
                .rsvpTextId(text.getId()).rsvpTextTitle(text.getTitle())
                .rsvpTextContent(text.getContent()).rsvpQuestions(questions)
                .totalCells(questions.size())
                .build();
        MatchFoundMessage msgForOpponent = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(user.getUsername()).exerciseType("rsvp")
                .rsvpSyntagmWidth(finalSyntagmWidth).rsvpDisplayTime(finalDisplayTime)
                .rsvpTextId(text.getId()).rsvpTextTitle(text.getTitle())
                .rsvpTextContent(text.getContent()).rsvpQuestions(questions)
                .totalCells(questions.size())
                .build();

        return new MatchData(session, msgForUser, msgForOpponent);
    }

    private MatchData buildSyntagmMatch(User user, MatchmakingQueue opponent, SyntagmQueueRequest req) {
        int finalSyntagmWidth = Math.min(req.getSyntagmWidth(), opponent.getRsvpSyntagmWidth());
        int finalDisplayTime  = Math.max(req.getDisplayTime(),  opponent.getRsvpDisplayTime());

        Text text = pickRandomText();
        List<QuestionResponse> questions = buildQuestionResponses(text);

        DuelSessionParams params = DuelSessionParams.builder()
                .totalCells(questions.size())
                .rsvpTextId(text.getId())
                .rsvpSyntagmWidth(finalSyntagmWidth)
                .rsvpDisplayTimeMs(finalDisplayTime)
                .build();
        DuelSession session = saveSessionWithParticipants(SYNTAGM, params, user, opponent.getUser());

        MatchFoundMessage msgForUser = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(opponent.getUser().getUsername()).exerciseType(SYNTAGM)
                .rsvpSyntagmWidth(finalSyntagmWidth).rsvpDisplayTime(finalDisplayTime)
                .rsvpTextId(text.getId()).rsvpTextTitle(text.getTitle())
                .rsvpTextContent(text.getContent()).rsvpQuestions(questions)
                .totalCells(questions.size())
                .build();
        MatchFoundMessage msgForOpponent = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(user.getUsername()).exerciseType(SYNTAGM)
                .rsvpSyntagmWidth(finalSyntagmWidth).rsvpDisplayTime(finalDisplayTime)
                .rsvpTextId(text.getId()).rsvpTextTitle(text.getTitle())
                .rsvpTextContent(text.getContent()).rsvpQuestions(questions)
                .totalCells(questions.size())
                .build();

        return new MatchData(session, msgForUser, msgForOpponent);
    }

    private MatchData buildSchulteMatch(User user, MatchmakingQueue opponent, SchulteQueueRequest req) {
        int finalGrid = Math.min(req.getGridSize(), opponent.getGridSize());
        int finalFont = Math.max(req.getFontSize(), opponent.getFontSize());
        int totalCells = finalGrid * finalGrid;
        int[] numbers = generateShuffledNumbers(totalCells);

        DuelSessionParams params = DuelSessionParams.builder()
                .totalCells(totalCells)
                .gridSize(finalGrid).fontSize(finalFont)
                .schulteNumbersJson(toJson(numbers))
                .build();
        DuelSession session = saveSessionWithParticipants(SCHULTE_TABLE, params, user, opponent.getUser());

        MatchFoundMessage msgForUser = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(opponent.getUser().getUsername()).exerciseType(SCHULTE_TABLE)
                .gridSize(finalGrid).fontSize(finalFont).numbers(numbers).totalCells(totalCells)
                .build();
        MatchFoundMessage msgForOpponent = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(user.getUsername()).exerciseType(SCHULTE_TABLE)
                .gridSize(finalGrid).fontSize(finalFont).numbers(numbers).totalCells(totalCells)
                .build();

        return new MatchData(session, msgForUser, msgForOpponent);
    }

    private MatchData buildWordSearchMatch(User user, MatchmakingQueue opponent,
                                            WordSearchQueueRequest req) {
        int finalRows      = Math.min(req.getWsRows(),      opponent.getWsRows());
        int finalCols      = Math.min(req.getWsCols(),      opponent.getWsCols());
        int finalWordCount = Math.min(req.getWsWordCount(), opponent.getWsWordCount());
        int finalFontSize  = Math.max(req.getWsFontSize(),  opponent.getWsFontSize());

        WordSearchGridData gridData = generateWordSearchGrid(finalRows, finalCols, finalWordCount);

        DuelSessionParams params = DuelSessionParams.builder()
                .totalCells(finalWordCount)
                .wsRows(finalRows).wsCols(finalCols)
                .wsWordCount(finalWordCount).wsFontSize(finalFontSize)
                .wsGridJson(toJsonObject(gridData.grid()))
                .wsWordsJson(toJsonObject(gridData.words()))
                .wsPositionsJson(toJsonObject(gridData.positions()))
                .build();
        DuelSession session = saveSessionWithParticipants(WORD_SEARCH, params, user, opponent.getUser());

        String[]       wsWords     = gridData.words().toArray(new String[0]);
        WsWordPosition[] wsPositions = gridData.positions().toArray(new WsWordPosition[0]);

        MatchFoundMessage msgForUser = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(opponent.getUser().getUsername()).exerciseType(WORD_SEARCH)
                .wsGrid(gridData.grid()).wsWords(wsWords).wsWordPositions(wsPositions)
                .wsRows(finalRows).wsCols(finalCols)
                .wsWordCount(finalWordCount).wsFontSize(finalFontSize)
                .totalCells(finalWordCount)
                .build();
        MatchFoundMessage msgForOpponent = MatchFoundMessage.builder()
                .type(MATCH_FOUND).sessionId(session.getId())
                .opponentName(user.getUsername()).exerciseType(WORD_SEARCH)
                .wsGrid(gridData.grid()).wsWords(wsWords).wsWordPositions(wsPositions)
                .wsRows(finalRows).wsCols(finalCols)
                .wsWordCount(finalWordCount).wsFontSize(finalFontSize)
                .totalCells(finalWordCount)
                .build();

        return new MatchData(session, msgForUser, msgForOpponent);
    }

    /** Generates a word-search grid: places words horizontally in unique random rows. */
    private WordSearchGridData generateWordSearchGrid(int rows, int cols, int wordCount) {
        List<String> shuffled = new ArrayList<>(wsWordBankRepository.findAllWords());
        Collections.shuffle(shuffled, RANDOM);
        List<String> fitting = shuffled.stream()
                .filter(w -> w.length() <= cols)
                .limit(wordCount)
                .toList();

        // Fill grid with random Ukrainian letters
        char[][] grid = new char[rows][cols];
        for (char[] row : grid) {
            for (int c = 0; c < cols; c++) {
                row[c] = UKRAINIAN_LETTERS.charAt(RANDOM.nextInt(UKRAINIAN_LETTERS.length()));
            }
        }

        // Place each word in a unique random row
        List<Integer> availableRows = new ArrayList<>();
        for (int i = 0; i < rows; i++) availableRows.add(i);
        Collections.shuffle(availableRows, RANDOM);

        List<WsWordPosition> positions = new ArrayList<>();
        for (int i = 0; i < fitting.size(); i++) {
            String word    = fitting.get(i);
            int    rowIdx  = availableRows.get(i);
            int    maxStart = cols - word.length();
            int    startCol = RANDOM.nextInt(maxStart + 1);
            for (int k = 0; k < word.length(); k++) {
                grid[rowIdx][startCol + k] = word.charAt(k);
            }
            positions.add(new WsWordPosition(word, rowIdx, startCol));
        }

        // Convert char[][] → String[][]
        String[][] stringGrid = new String[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                stringGrid[r][c] = String.valueOf(grid[r][c]);
            }
        }
        return new WordSearchGridData(stringGrid, fitting, positions);
    }

    private record WordSearchGridData(String[][] grid, List<String> words, List<WsWordPosition> positions) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WordSearchGridData other)) return false;
            return Arrays.deepEquals(this.grid, other.grid)
                    && Objects.equals(this.words, other.words)
                    && Objects.equals(this.positions, other.positions);
        }

        @Override
        public int hashCode() {
            int result = Arrays.deepHashCode(grid);
            result = 31 * result + Objects.hashCode(words);
            result = 31 * result + Objects.hashCode(positions);
            return result;
        }

        @Override
        public String toString() {
            return "WordSearchGridData[grid=" + Arrays.deepToString(grid)
                    + ", words=" + words
                    + ", positions=" + positions + "]";
        }
    }

    // ── Infrastructure helpers ────────────────────────────────────────────────

    /** Finds the user by email and removes any stale queue entry for them. */
    private User findAndCleanupUser(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        queueRepository.deleteByUserId(user.getId());
        return user;
    }

    /** Looks up the oldest waiting opponent for the given exercise type. */
    private Optional<MatchmakingQueue> findOpponent(Long userId, String exerciseType) {
        List<MatchmakingQueue> opponents =
                queueRepository.findOpponents(userId, exerciseType, PageRequest.of(0, 1));
        return opponents.isEmpty() ? Optional.empty() : Optional.of(opponents.get(0));
    }

    /**
     * Creates a DuelSession (with cascaded DuelSessionParams) and saves both participants.
     * The params object must NOT have session set yet — this method wires the relationship.
     */
    private DuelSession saveSessionWithParticipants(String exerciseType, DuelSessionParams params,
                                                    User user, User opponent) {
        DuelSession session = DuelSession.builder()
                .exerciseType(exerciseType)
                .status("WAITING")
                .build();
        params.setSession(session);
        session.setParams(params);
        session = sessionRepository.save(session);

        participantRepository.save(DuelParticipant.builder().session(session).user(user).build());
        participantRepository.save(DuelParticipant.builder().session(session).user(opponent).build());
        return session;
    }

    /** Adds the user to the matchmaking queue when no opponent was found immediately. */
    private void addToQueue(User user, JoinQueueRequest req) {
        MatchmakingQueue entry;
        if (req instanceof SchulteQueueRequest sr) {
            entry = MatchmakingQueue.builder()
                    .user(user).exerciseType(SCHULTE_TABLE)
                    .gridSize(sr.getGridSize()).fontSize(sr.getFontSize())
                    .build();
        } else if (req instanceof NumbersQueueRequest nr) {
            entry = MatchmakingQueue.builder()
                    .user(user).exerciseType(NUMBERS)
                    .digitCount(nr.getDigitCount()).displayTime(nr.getDisplayTime())
                    .build();
        } else if (req instanceof WordPairsQueueRequest wp) {
            entry = MatchmakingQueue.builder()
                    .user(user).exerciseType(WORD_PAIRS)
                    .wpRows(wp.getWpRows()).wpCols(wp.getWpCols())
                    .wpTimeLimit(wp.getWpTimeLimit()).wpFontSize(wp.getWpFontSize())
                    .build();
        } else if (req instanceof RsvpQueueRequest rr) {
            entry = MatchmakingQueue.builder()
                    .user(user).exerciseType("rsvp")
                    .rsvpSyntagmWidth(rr.getRsvpSyntagmWidth()).rsvpDisplayTime(rr.getRsvpDisplayTime())
                    .build();
        } else if (req instanceof WordSearchQueueRequest ws) {
            entry = MatchmakingQueue.builder()
                    .user(user).exerciseType(WORD_SEARCH)
                    .wsRows(ws.getWsRows()).wsCols(ws.getWsCols())
                    .wsWordCount(ws.getWsWordCount()).wsFontSize(ws.getWsFontSize())
                    .build();
        } else if (req instanceof SyntagmQueueRequest sr) {
            entry = MatchmakingQueue.builder()
                    .user(user).exerciseType(SYNTAGM)
                    .rsvpSyntagmWidth(sr.getSyntagmWidth()).rsvpDisplayTime(sr.getDisplayTime())
                    .build();
        } else {
            throw new IllegalArgumentException("Unknown exercise type: " + req.getExerciseType());
        }
        queueRepository.save(entry);
    }

    /** Picks a random text from DB for RSVP exercise. */
    private Text pickRandomText() {
        List<Long> textIds = textRepository.findAllIds();
        if (textIds.isEmpty()) {
            throw new ResourceNotFoundException("No texts available for RSVP duel");
        }
        Long randomId = textIds.get(RANDOM.nextInt(textIds.size()));
        return textRepository.findById(randomId)
                .orElseThrow(() -> new ResourceNotFoundException("Text not found: " + randomId));
    }

    /** Maps Text questions to QuestionResponse DTOs. */
    private List<QuestionResponse> buildQuestionResponses(Text text) {
        return text.getQuestions().stream()
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .text(q.getQuestionText())
                        .options(q.getOptions().stream().map(opt -> opt.getOptionText()).toList())
                        .correctIndex(q.getCorrectIndex())
                        .build())
                .toList();
    }

    /** Carries a created session and the two MATCH_FOUND messages out of each exercise builder. */
    private record MatchData(DuelSession session, MatchFoundMessage msgForUser, MatchFoundMessage msgForOpponent) {}

    // ── Queue lifecycle ───────────────────────────────────────────────────────

    @Transactional
    public void leaveQueue(String username) {
        userRepository.findByEmail(username)
                .ifPresent(u -> queueRepository.deleteByUserId(u.getId()));
    }

    @Transactional
    public void cleanupStaleQueueEntries() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusMinutes(5);
        queueRepository.deleteAllByJoinedAtBefore(cutoff);
    }

    // ── Game data generators ──────────────────────────────────────────────────

    /** Generates a shuffled grid of word-pairs from DB: ~50% different. */
    private List<WordPairDto> generateWordPairs(int totalCells) {
        int diffCount = totalCells / 2;
        int sameCount = totalCells - diffCount;

        List<WordPairDto> pairs = new ArrayList<>();

        wpDiffPairRepository.findRandom(diffCount)
                .stream()
                // Defensive guard: identical words or Latin homoglyphs must never
                // reach the client as "different" pairs (mirrors WordPairsController).
                .filter(p -> isValidDiffPair(p.getWord1(), p.getWord2()))
                .forEach(p -> pairs.add(new WordPairDto(p.getWord1(), p.getWord2(), true)));

        wpSameWordRepository.findRandom(sameCount)
                .forEach(p -> pairs.add(new WordPairDto(p.getWord(), p.getWord(), false)));

        Collections.shuffle(pairs, RANDOM);
        return pairs;
    }

    private static boolean isValidDiffPair(String w1, String w2) {
        if (w1.equals(w2)) return false;
        return !containsLatinHomoglyph(w1) && !containsLatinHomoglyph(w2);
    }

    private static boolean containsLatinHomoglyph(String word) {
        for (char ch : word.toCharArray()) {
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) return true;
        }
        return false;
    }

    /** Generates count shuffled numbers 1..count (for Schulte Table). */
    private int[] generateShuffledNumbers(int count) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) list.add(i);
        Collections.shuffle(list);
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Generates NUMBERS_TOTAL_ROUNDS random numbers each having exactly digitCount digits. */
    private int[] generateRandomNumbers(int digitCount) {
        int min = (int) Math.pow(10, digitCount - 1.0);
        int max = (int) Math.pow(10, digitCount) - 1;
        int[] result = new int[NUMBERS_TOTAL_ROUNDS];
        for (int i = 0; i < NUMBERS_TOTAL_ROUNDS; i++) {
            result[i] = min + RANDOM.nextInt(max - min + 1);
        }
        return result;
    }

    private String toJson(int[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private String toJsonWordPairs(WordPairDto[] pairs) {
        try {
            return objectMapper.writeValueAsString(pairs);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to serialize word pairs to JSON", e);
        }
    }

    private String toJsonObject(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to serialize object to JSON", e);
        }
    }
}

