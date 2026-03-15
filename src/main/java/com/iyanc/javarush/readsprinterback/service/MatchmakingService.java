package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.request.JoinQueueRequest;
import com.iyanc.javarush.readsprinterback.dto.response.DuelEventMessage;
import com.iyanc.javarush.readsprinterback.dto.response.MatchFoundMessage;
import com.iyanc.javarush.readsprinterback.repository.DuelSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final MatchmakingDbService matchmakingDbService;
    private final DuelSessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Carries all data needed for WebSocket notifications out of the @Transactional boundary.
     * Public so that MatchmakingDbService can instantiate it.
     */
    public record MatchResult(
            Long sessionId,
            String userEmail,
            String opponentEmail,
            MatchFoundMessage msgForUser,
            MatchFoundMessage msgForOpponent
    ) {}

    // ─── Join queue ───────────────────────────────────────────────────────────

    /**
     * NOT @Transactional intentionally.
     * DB work is delegated to MatchmakingDbService (separate Spring bean = separate proxy).
     * WebSocket notifications are sent AFTER the transaction is committed,
     * ensuring DuelParticipant rows are visible when the client reacts to MATCH_FOUND.
     */
    public Map<String, Object> joinQueue(String username, JoinQueueRequest req) {
        // Step 1: all DB work in a @Transactional method on a different bean
        MatchResult match = matchmakingDbService.tryCreateMatch(username, req);

        if (match != null) {
            // Step 2: transaction committed — safe to send WebSocket messages now
            log.info("Match found: session={} user={} opponent={}",
                    match.sessionId(), match.userEmail(), match.opponentEmail());

            messagingTemplate.convertAndSendToUser(
                    match.userEmail(), "/queue/duel", match.msgForUser());
            messagingTemplate.convertAndSendToUser(
                    match.opponentEmail(), "/queue/duel", match.msgForOpponent());

            // Start countdown asynchronously
            startCountdown(match.sessionId());

            return Map.of("status", "matched", "sessionId", match.sessionId());
        }

        return Map.of("status", "waiting");
    }

    // ─── Leave queue ──────────────────────────────────────────────────────────

    public void leaveQueue(String username) {
        matchmakingDbService.leaveQueue(username);
    }

    // ─── Countdown + Start ────────────────────────────────────────────────────

    private void startCountdown(Long sessionId) {
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
    public void cleanupStaleQueueEntries() {
        matchmakingDbService.cleanupStaleQueueEntries();
    }
}

