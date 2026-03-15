package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.request.DuelFinishMessage;
import com.iyanc.javarush.readsprinterback.dto.request.DuelLeaveMessage;
import com.iyanc.javarush.readsprinterback.dto.request.DuelProgressMessage;
import com.iyanc.javarush.readsprinterback.dto.response.DuelEventMessage;
import com.iyanc.javarush.readsprinterback.entity.DuelParticipant;
import com.iyanc.javarush.readsprinterback.entity.DuelSession;
import com.iyanc.javarush.readsprinterback.entity.User;
import com.iyanc.javarush.readsprinterback.repository.DuelParticipantRepository;
import com.iyanc.javarush.readsprinterback.repository.DuelSessionRepository;
import com.iyanc.javarush.readsprinterback.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuelService {

    private final DuelSessionRepository sessionRepository;
    private final DuelParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MatchmakingService matchmakingService;

    // ─── Progress update ─────────────────────────────────────────────────────

    @Transactional
    public void handleProgress(String username, DuelProgressMessage msg) {
        User user = getUser(username);
        DuelSession session = getSession(msg.getSessionId());

        // Update this user's participant record
        participantRepository.findBySessionIdAndUserId(session.getId(), user.getId())
                .ifPresent(p -> {
                    p.setProgress(msg.getProgress());
                    p.setErrors(msg.getErrors());
                    participantRepository.save(p);
                });

        // Find opponent and notify them
        getOpponentParticipant(session, user.getId()).ifPresent(opponent -> {
            DuelEventMessage event = DuelEventMessage.builder()
                    .type("OPPONENT_PROGRESS")
                    .opponentName(user.getUsername())
                    .opponentProgress(msg.getProgress())
                    .totalCells(session.getGridSize() * session.getGridSize())
                    .build();
            matchmakingService.broadcast(session.getId(), event);
        });
    }

    // ─── Finish ───────────────────────────────────────────────────────────────

    @Transactional
    public void handleFinish(String username, DuelFinishMessage msg) {
        User user = getUser(username);
        DuelSession session = getSession(msg.getSessionId());

        // Update participant as finished
        participantRepository.findBySessionIdAndUserId(session.getId(), user.getId())
                .ifPresent(p -> {
                    p.setProgress(msg.getProgress());
                    p.setErrors(msg.getErrors());
                    p.setDurationMs(msg.getDurationMs());
                    p.setScore(msg.getScore());
                    p.setFinished(true);
                    p.setFinishedAt(LocalDateTime.now());
                    participantRepository.save(p);
                });

        // Notify opponent
        getOpponentParticipant(session, user.getId()).ifPresent(opponent -> {
            DuelEventMessage event = DuelEventMessage.builder()
                    .type("OPPONENT_FINISHED")
                    .opponentName(user.getUsername())
                    .opponentDurationMs(msg.getDurationMs())
                    .opponentErrors(msg.getErrors())
                    .opponentScore(msg.getScore())
                    .build();
            matchmakingService.broadcast(session.getId(), event);
        });

        // Check if both finished → send SESSION_RESULT
        checkAndSendSessionResult(session);
    }

    // ─── Leave ────────────────────────────────────────────────────────────────

    @Transactional
    public void handleLeave(String username, DuelLeaveMessage msg) {
        User user = getUser(username);
        DuelSession session = getSession(msg.getSessionId());

        // Notify opponent
        DuelEventMessage event = DuelEventMessage.builder()
                .type("OPPONENT_LEFT")
                .opponentName(user.getUsername())
                .build();
        matchmakingService.broadcast(session.getId(), event);

        log.info("User {} left duel session {}", username, session.getId());
    }

    // ─── Disconnect (called from WebSocket listener) ──────────────────────────

    @Transactional
    public void handleDisconnect(String username) {
        User user = getUser(username);

        // Find all active sessions this user is in
        List<DuelParticipant> activeParts = participantRepository.findAllByUserId(user.getId())
                .stream()
                .filter(p -> !p.isFinished())
                .toList();

        for (DuelParticipant p : activeParts) {
            DuelSession session = p.getSession();
            if ("ACTIVE".equals(session.getStatus()) || "COUNTDOWN".equals(session.getStatus())) {
                p.setDisconnected(true);
                participantRepository.save(p);

                DuelEventMessage event = DuelEventMessage.builder()
                        .type("OPPONENT_DISCONNECTED")
                        .opponentName(user.getUsername())
                        .build();
                matchmakingService.broadcast(session.getId(), event);
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void checkAndSendSessionResult(DuelSession session) {
        List<DuelParticipant> participants =
                participantRepository.findAllBySessionId(session.getId());

        boolean allDone = participants.stream()
                .allMatch(p -> p.isFinished() || p.isDisconnected());

        if (allDone && participants.size() == 2) {
            session.setStatus("FINISHED");
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);

            DuelParticipant p1 = participants.get(0);
            DuelParticipant p2 = participants.get(1);

            DuelEventMessage.ParticipantResult r1 = toResult(p1, session);
            DuelEventMessage.ParticipantResult r2 = toResult(p2, session);

            // Broadcast to both — each side will figure out which is "my" vs "opponent"
            DuelEventMessage event = DuelEventMessage.builder()
                    .type("SESSION_RESULT")
                    .myResult(r1)
                    .opponentResult(r2)
                    .build();
            matchmakingService.broadcast(session.getId(), event);
        }
    }

    private DuelEventMessage.ParticipantResult toResult(DuelParticipant p, DuelSession session) {
        return DuelEventMessage.ParticipantResult.builder()
                .username(p.getUser().getUsername())
                .durationMs(p.getDurationMs())
                .errors(p.getErrors())
                .score(p.getScore() != null ? p.getScore() : 0)
                .progress(p.getProgress())
                .totalCells(session.getGridSize() * session.getGridSize())
                .finished(p.isFinished())
                .disconnected(p.isDisconnected())
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private DuelSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }

    private java.util.Optional<DuelParticipant> getOpponentParticipant(DuelSession session, Long userId) {
        return participantRepository.findAllBySessionId(session.getId())
                .stream()
                .filter(p -> !p.getUser().getId().equals(userId))
                .findFirst();
    }
}

