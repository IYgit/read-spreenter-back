package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.request.DuelFinishMessage;
import com.iyanc.javarush.readsprinterback.dto.request.DuelLeaveMessage;
import com.iyanc.javarush.readsprinterback.dto.request.DuelProgressMessage;
import com.iyanc.javarush.readsprinterback.dto.response.DuelEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuelService {

    private final DuelDbService duelDbService;
    private final MatchmakingService matchmakingService;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Progress update ─────────────────────────────────────────────────────

    /**
     * NOT @Transactional — DB work is in DuelDbService.
     * OPPONENT_PROGRESS is sent only to the opponent (convertAndSendToUser),
     * NOT broadcast — otherwise the sender would receive their own progress
     * as "opponent progress" and overwrite the opponent's progress bar.
     */
    public void handleProgress(String username, DuelProgressMessage msg) {
        duelDbService.saveProgress(username, msg).ifPresent(result -> {
            if (result.opponentEmail() != null) {
                messagingTemplate.convertAndSendToUser(
                        result.opponentEmail(), "/queue/duel", result.eventForOpponent());
            }
        });
    }

    // ─── Finish ───────────────────────────────────────────────────────────────

    /**
     * NOT @Transactional — DB work (save score, check all finished) is in DuelDbService.
     * After commit:
     *   1. Send OPPONENT_FINISHED only to the opponent.
     *   2. If both finished — broadcast SESSION_RESULT to both.
     */
    public void handleFinish(String username, DuelFinishMessage msg) {
        DuelDbService.FinishResult result = duelDbService.saveFinish(username, msg);

        // Notify opponent that this player finished
        if (result.opponentEmail() != null && result.eventForOpponent() != null) {
            messagingTemplate.convertAndSendToUser(
                    result.opponentEmail(), "/queue/duel", result.eventForOpponent());
        }

        // If both finished — send final results to both via broadcast
        if (result.sessionResult() != null) {
            DuelDbService.SessionResult sr = result.sessionResult();
            DuelEventMessage sessionResultEvent = DuelEventMessage.builder()
                    .type("SESSION_RESULT")
                    .myResult(sr.r1())
                    .opponentResult(sr.r2())
                    .build();
            log.info("Session {} finished — sending SESSION_RESULT", sr.sessionId());
            matchmakingService.broadcast(sr.sessionId(), sessionResultEvent);
        }
    }

    // ─── Leave ────────────────────────────────────────────────────────────────

    public void handleLeave(String username, DuelLeaveMessage msg) {
        duelDbService.getOpponentEmailForLeave(username, msg.getSessionId())
                .ifPresent(opponentEmail -> {
                    DuelEventMessage event = DuelEventMessage.builder()
                            .type("OPPONENT_LEFT")
                            .opponentName(username)
                            .build();
                    messagingTemplate.convertAndSendToUser(opponentEmail, "/queue/duel", event);
                });
        log.info("User {} left duel session {}", username, msg.getSessionId());
    }

    // ─── Disconnect (called from WebSocketEventListener) ──────────────────────

    public void handleDisconnect(String username) {
        duelDbService.saveDisconnect(username).forEach(result -> {
            if (result.opponentEmail() != null) {
                messagingTemplate.convertAndSendToUser(
                        result.opponentEmail(), "/queue/duel", result.eventForOpponent());
            }
        });
    }
}
