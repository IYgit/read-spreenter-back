package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.request.JoinQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.response.MatchFoundMessage;
import com.iyanc.javarush.readsprinterback.entity.*;
import com.iyanc.javarush.readsprinterback.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Handles all DB operations for matchmaking in a dedicated @Transactional boundary.
 * Separated from MatchmakingService so that Spring AOP proxy intercepts the transaction
 * correctly — self-invocation within the same bean would bypass the proxy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingDbService {

    private final MatchmakingQueueRepository queueRepository;
    private final DuelSessionRepository sessionRepository;
    private final DuelParticipantRepository participantRepository;
    private final UserRepository userRepository;

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

            // Determine final parameters
            int finalGrid = Math.min(req.getGridSize(), opponent.getGridSize());
            int finalFont = Math.max(req.getFontSize(), opponent.getFontSize());
            String finalExercise = "schulte-table"; // only supported type currently

            // Generate shared number sequence
            int[] numbers = generateShuffledNumbers(finalGrid * finalGrid);
            String numbersJson = toJson(numbers);

            // Create session
            DuelSession session = DuelSession.builder()
                    .exerciseType(finalExercise)
                    .gridSize(finalGrid)
                    .fontSize(finalFont)
                    .numbersSequence(numbersJson)
                    .status("WAITING")
                    .build();
            session = sessionRepository.save(session);

            // Create participants
            DuelParticipant p1 = DuelParticipant.builder()
                    .session(session).user(user).build();
            DuelParticipant p2 = DuelParticipant.builder()
                    .session(session).user(opponent.getUser()).build();
            participantRepository.save(p1);
            participantRepository.save(p2);

            MatchFoundMessage msgForUser = MatchFoundMessage.builder()
                    .type("MATCH_FOUND")
                    .sessionId(session.getId())
                    .opponentName(opponent.getUser().getUsername())
                    .exerciseType(finalExercise)
                    .gridSize(finalGrid)
                    .fontSize(finalFont)
                    .numbers(numbers)
                    .totalCells(finalGrid * finalGrid)
                    .build();

            MatchFoundMessage msgForOpponent = MatchFoundMessage.builder()
                    .type("MATCH_FOUND")
                    .sessionId(session.getId())
                    .opponentName(user.getUsername())
                    .exerciseType(finalExercise)
                    .gridSize(finalGrid)
                    .fontSize(finalFont)
                    .numbers(numbers)
                    .totalCells(finalGrid * finalGrid)
                    .build();

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

    private int[] generateShuffledNumbers(int count) {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) list.add(i);
        Collections.shuffle(list);
        return list.stream().mapToInt(Integer::intValue).toArray();
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
}

