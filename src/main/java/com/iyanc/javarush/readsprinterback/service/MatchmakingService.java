package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.request.JoinQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.response.DuelEventMessage;
import com.iyanc.javarush.readsprinterback.dto.response.MatchFoundMessage;
import com.iyanc.javarush.readsprinterback.entity.*;
import com.iyanc.javarush.readsprinterback.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.data.domain.PageRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final MatchmakingQueueRepository queueRepository;
    private final DuelSessionRepository sessionRepository;
    private final DuelParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Join queue ───────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> joinQueue(String username, JoinQueueRequest req) {
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

            // Notify both players via personal WebSocket queue
            // Note: convertAndSendToUser uses principal name = email (set by UserDetailsServiceImpl)
            // opponentName uses the display username field for the UI
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

            // Route by email (= Spring Security principal name)
            messagingTemplate.convertAndSendToUser(
                    user.getEmail(), "/queue/duel", msgForUser);
            messagingTemplate.convertAndSendToUser(
                    opponent.getUser().getEmail(), "/queue/duel", msgForOpponent);

            // Start countdown asynchronously
            startCountdown(session.getId(), user.getEmail(), opponent.getUser().getEmail());

            return Map.of("status", "matched", "sessionId", session.getId());
        }

        // No opponent found — add to queue
        MatchmakingQueue entry = MatchmakingQueue.builder()
                .user(user)
                .exerciseType(req.getExerciseType())
                .gridSize(req.getGridSize())
                .fontSize(req.getFontSize())
                .build();
        queueRepository.save(entry);

        return Map.of("status", "waiting");
    }

    // ─── Leave queue ──────────────────────────────────────────────────────────

    @Transactional
    public void leaveQueue(String username) {
        userRepository.findByEmail(username)
                .ifPresent(u -> queueRepository.deleteByUserId(u.getId()));
    }

    // ─── Countdown + Start ────────────────────────────────────────────────────

    private void startCountdown(Long sessionId, String user1, String user2) {
        new Thread(() -> {
            try {
                for (int i = 3; i >= 1; i--) {
                    DuelEventMessage msg = DuelEventMessage.builder()
                            .type("COUNTDOWN").countdown(i).build();
                    broadcast(sessionId, msg);
                    Thread.sleep(1000);
                }

                // Update session status
                sessionRepository.findById(sessionId).ifPresent(s -> {
                    s.setStatus("ACTIVE");
                    s.setStartedAt(LocalDateTime.now());
                    sessionRepository.save(s);
                });

                DuelEventMessage startMsg = DuelEventMessage.builder().type("START").build();
                broadcast(sessionId, startMsg);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Countdown interrupted for session {}", sessionId);
            }
        }).start();
    }

    // ─── Broadcast helper ─────────────────────────────────────────────────────

    public void broadcast(Long sessionId, DuelEventMessage msg) {
        messagingTemplate.convertAndSend("/topic/duel/" + sessionId, msg);
    }

    // ─── Cleanup stale queue entries (TTL 5 min) ──────────────────────────────

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cleanupStaleQueueEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        queueRepository.deleteAllByJoinedAtBefore(cutoff);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

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

