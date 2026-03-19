package com.iyanc.javarush.readsprinterback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyanc.javarush.readsprinterback.dto.WordPairDto;
import com.iyanc.javarush.readsprinterback.dto.request.JoinQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.response.MatchFoundMessage;
import com.iyanc.javarush.readsprinterback.dto.response.QuestionResponse;
import com.iyanc.javarush.readsprinterback.entity.*;
import com.iyanc.javarush.readsprinterback.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private static final String MATCH_FOUND = "MATCH_FOUND";
    private static final Random RANDOM = new Random();

    private final MatchmakingQueueRepository queueRepository;
    private final DuelSessionRepository sessionRepository;
    private final DuelParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final WpDiffPairRepository wpDiffPairRepository;
    private final WpSameWordRepository wpSameWordRepository;
    private final TextRepository textRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * All DB operations in a single transaction.
     * Returns MatchResult if a match was created, null if added to waiting queue.
     */
    @Transactional
    public MatchmakingService.MatchResult tryCreateMatch(String username, JoinQueueRequest req) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Remove any stale entry for this user
        queueRepository.deleteByUserId(user.getId());

        // Try to find an opponent immediately
        List<MatchmakingQueue> opponents =
                queueRepository.findOpponents(user.getId(), req.getExerciseType(), PageRequest.of(0, 1));
        Optional<MatchmakingQueue> opponentOpt = opponents.isEmpty()
                ? Optional.empty()
                : Optional.of(opponents.get(0));

        if (opponentOpt.isPresent()) {
            MatchmakingQueue opponent = opponentOpt.get();
            queueRepository.delete(opponent);

            String exerciseType = req.getExerciseType();

            MatchFoundMessage msgForUser;
            MatchFoundMessage msgForOpponent;
            DuelSession session;

            if ("numbers".equals(exerciseType)) {
                // ── Numbers exercise ────────────────────────────────────────────
                int finalDigitCount = Math.min(req.getDigitCount(), opponent.getDigitCount());
                int finalDisplayTime = Math.max(req.getDisplayTime(), opponent.getDisplayTime());

                // Generate array of NUMBERS_TOTAL_ROUNDS random numbers with finalDigitCount digits
                int[] numbers = generateRandomNumbers(finalDigitCount);
                String numbersJson = toJson(numbers);

                session = DuelSession.builder()
                        .exerciseType(exerciseType)
                        .gridSize(finalDigitCount)   // reuse gridSize column to store digitCount
                        .fontSize(finalDisplayTime)  // reuse fontSize column to store displayTime
                        .numbersSequence(numbersJson)
                        .status("WAITING")
                        .build();
                session = sessionRepository.save(session);

                DuelParticipant p1 = DuelParticipant.builder().session(session).user(user).build();
                DuelParticipant p2 = DuelParticipant.builder().session(session).user(opponent.getUser()).build();
                participantRepository.save(p1);
                participantRepository.save(p2);

                msgForUser = MatchFoundMessage.builder()
                        .type(MATCH_FOUND)
                        .sessionId(session.getId())
                        .opponentName(opponent.getUser().getUsername())
                        .exerciseType(exerciseType)
                        .digitCount(finalDigitCount)
                        .displayTime(finalDisplayTime)
                        .totalRounds(NUMBERS_TOTAL_ROUNDS)
                        .numbers(numbers)
                        .totalCells(NUMBERS_TOTAL_ROUNDS)
                        .build();

                msgForOpponent = MatchFoundMessage.builder()
                        .type(MATCH_FOUND)
                        .sessionId(session.getId())
                        .opponentName(user.getUsername())
                        .exerciseType(exerciseType)
                        .digitCount(finalDigitCount)
                        .displayTime(finalDisplayTime)
                        .totalRounds(NUMBERS_TOTAL_ROUNDS)
                        .numbers(numbers)
                        .totalCells(NUMBERS_TOTAL_ROUNDS)
                        .build();

            } else if ("word-pairs".equals(exerciseType)) {
                // ── Word Pairs exercise ─────────────────────────────────────────
                int finalRows = Math.min(req.getWpRows(), opponent.getWpRows());
                int finalCols = Math.min(req.getWpCols(), opponent.getWpCols());
                int finalTimeLimit = Math.max(req.getWpTimeLimit(), opponent.getWpTimeLimit());
                int finalFontSize = Math.max(req.getWpFontSize(), opponent.getWpFontSize());

                List<WordPairDto> pairsList = generateWordPairs(finalRows * finalCols);
                int diffCount = (int) pairsList.stream().filter(WordPairDto::isDiff).count();
                WordPairDto[] pairs = pairsList.toArray(new WordPairDto[0]);
                String pairsJson = toJsonWordPairs(pairs);

                // gridSize encodes rows*10+cols (e.g. 4 rows, 4 cols → 44)
                // fontSize stores wpFontSize; wpTimeLimit stored in display_time (reuse)
                session = DuelSession.builder()
                        .exerciseType("word-pairs")
                        .gridSize(finalRows * 10 + finalCols)
                        .fontSize(finalFontSize)
                        .numbersSequence(pairsJson)
                        .status("WAITING")
                        .build();
                session = sessionRepository.save(session);

                DuelParticipant p1 = DuelParticipant.builder().session(session).user(user).build();
                DuelParticipant p2 = DuelParticipant.builder().session(session).user(opponent.getUser()).build();
                participantRepository.save(p1);
                participantRepository.save(p2);

                msgForUser = MatchFoundMessage.builder()
                        .type(MATCH_FOUND)
                        .sessionId(session.getId())
                        .opponentName(opponent.getUser().getUsername())
                        .exerciseType("word-pairs")
                        .pairs(pairs).wpRows(finalRows).wpCols(finalCols)
                        .wpTimeLimit(finalTimeLimit).wpFontSize(finalFontSize)
                        .totalCells(diffCount).build();

                msgForOpponent = MatchFoundMessage.builder()
                        .type(MATCH_FOUND)
                        .sessionId(session.getId())
                        .opponentName(user.getUsername())
                        .exerciseType("word-pairs")
                        .pairs(pairs).wpRows(finalRows).wpCols(finalCols)
                        .wpTimeLimit(finalTimeLimit).wpFontSize(finalFontSize)
                        .totalCells(diffCount).build();

            } else if ("rsvp".equals(exerciseType)) {
                // ── RSVP exercise ───────────────────────────────────────────────
                int finalSyntagmWidth = Math.min(req.getRsvpSyntagmWidth(), opponent.getRsvpSyntagmWidth());
                int finalDisplayTime  = Math.max(req.getRsvpDisplayTime(), opponent.getRsvpDisplayTime());

                // Pick a random text from DB
                List<Text> randomTexts = textRepository.findRandom(PageRequest.of(0, 1));
                if (randomTexts.isEmpty()) {
                    throw new RuntimeException("No texts available for RSVP duel");
                }
                Text text = randomTexts.get(0);
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
                int totalQuestions = questions.size();

                // Store: gridSize=textId, fontSize=displayTime, numbersSequence=syntagmWidth
                session = DuelSession.builder()
                        .exerciseType("rsvp")
                        .gridSize((int) text.getId().longValue())
                        .fontSize(finalDisplayTime)
                        .numbersSequence(String.valueOf(finalSyntagmWidth))
                        .status("WAITING")
                        .build();
                session = sessionRepository.save(session);

                DuelParticipant p1 = DuelParticipant.builder().session(session).user(user).build();
                DuelParticipant p2 = DuelParticipant.builder().session(session).user(opponent.getUser()).build();
                participantRepository.save(p1);
                participantRepository.save(p2);

                msgForUser = MatchFoundMessage.builder()
                        .type(MATCH_FOUND)
                        .sessionId(session.getId())
                        .opponentName(opponent.getUser().getUsername())
                        .exerciseType("rsvp")
                        .rsvpSyntagmWidth(finalSyntagmWidth)
                        .rsvpDisplayTime(finalDisplayTime)
                        .rsvpTextId(text.getId())
                        .rsvpTextTitle(text.getTitle())
                        .rsvpTextContent(text.getContent())
                        .rsvpQuestions(questions)
                        .totalCells(totalQuestions)
                        .build();

                msgForOpponent = MatchFoundMessage.builder()
                        .type(MATCH_FOUND)
                        .sessionId(session.getId())
                        .opponentName(user.getUsername())
                        .exerciseType("rsvp")
                        .rsvpSyntagmWidth(finalSyntagmWidth)
                        .rsvpDisplayTime(finalDisplayTime)
                        .rsvpTextId(text.getId())
                        .rsvpTextTitle(text.getTitle())
                        .rsvpTextContent(text.getContent())
                        .rsvpQuestions(questions)
                        .totalCells(totalQuestions)
                        .build();

            } else {
                int finalGrid = Math.min(req.getGridSize(), opponent.getGridSize());
                int finalFont = Math.max(req.getFontSize(), opponent.getFontSize());
                String finalExercise = "schulte-table";

                int[] numbers = generateShuffledNumbers(finalGrid * finalGrid);
                String numbersJson = toJson(numbers);

                session = DuelSession.builder()
                        .exerciseType(finalExercise)
                        .gridSize(finalGrid)
                        .fontSize(finalFont)
                        .numbersSequence(numbersJson)
                        .status("WAITING")
                        .build();
                session = sessionRepository.save(session);

                DuelParticipant p1 = DuelParticipant.builder().session(session).user(user).build();
                DuelParticipant p2 = DuelParticipant.builder().session(session).user(opponent.getUser()).build();
                participantRepository.save(p1);
                participantRepository.save(p2);

                msgForUser = MatchFoundMessage.builder()
                        .type(MATCH_FOUND)
                        .sessionId(session.getId())
                        .opponentName(opponent.getUser().getUsername())
                        .exerciseType(finalExercise)
                        .gridSize(finalGrid)
                        .fontSize(finalFont)
                        .numbers(numbers)
                        .totalCells(finalGrid * finalGrid)
                        .build();

                msgForOpponent = MatchFoundMessage.builder()
                        .type(MATCH_FOUND)
                        .sessionId(session.getId())
                        .opponentName(user.getUsername())
                        .exerciseType(finalExercise)
                        .gridSize(finalGrid)
                        .fontSize(finalFont)
                        .numbers(numbers)
                        .totalCells(finalGrid * finalGrid)
                        .build();
            }

            return new MatchmakingService.MatchResult(
                    session.getId(),
                    user.getEmail(),
                    opponent.getUser().getEmail(),
                    msgForUser,
                    msgForOpponent
            );
        }

        // No opponent found — add to queue
        MatchmakingQueue entry = MatchmakingQueue.builder()
                .user(user)
                .exerciseType(req.getExerciseType())
                .gridSize(req.getGridSize())
                .fontSize(req.getFontSize())
                .digitCount(req.getDigitCount())
                .displayTime(req.getDisplayTime())
                .wpRows(req.getWpRows())
                .wpCols(req.getWpCols())
                .wpTimeLimit(req.getWpTimeLimit())
                .wpFontSize(req.getWpFontSize())
                .rsvpSyntagmWidth(req.getRsvpSyntagmWidth())
                .rsvpDisplayTime(req.getRsvpDisplayTime())
                .build();
        queueRepository.save(entry);

        return null;
    }

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

    /** Generates a shuffled grid of word-pairs from DB: ~50% different. */
    private List<WordPairDto> generateWordPairs(int totalCells) {
        int diffCount = totalCells / 2;
        int sameCount = totalCells - diffCount;

        List<WordPairDto> pairs = new ArrayList<>();

        wpDiffPairRepository.findRandom(diffCount)
                .forEach(p -> pairs.add(new WordPairDto(p.getWord1(), p.getWord2(), true)));

        wpSameWordRepository.findRandom(sameCount)
                .forEach(p -> pairs.add(new WordPairDto(p.getWord(), p.getWord(), false)));

        Collections.shuffle(pairs, RANDOM);
        return pairs;
    }

    /** Generates count shuffled numbers 1..count (for Schulte Table). */
    private int[] generateShuffledNumbers(int count) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) list.add(i);
        Collections.shuffle(list);
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Generates NUMBERS_TOTAL_ROUNDS random numbers each having exactly digitCount digits (for Numbers exercise). */
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
            throw new RuntimeException("Failed to serialize word pairs", e);
        }
    }
}
