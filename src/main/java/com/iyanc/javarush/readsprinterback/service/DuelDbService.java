package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.request.DuelFinishMessage;
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
import java.util.Optional;

/**
 * Handles all DB operations for duel events in a dedicated @Transactional boundary.
 * Separated from DuelService so that WebSocket notifications are sent AFTER commit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DuelDbService {

    private final DuelSessionRepository sessionRepository;
    private final DuelParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public record ProgressResult(
            String opponentEmail,
            DuelEventMessage eventForOpponent
    ) {}

    public record FinishResult(
            String opponentEmail,
            DuelEventMessage eventForOpponent,
            SessionResult sessionResult  // null if not both finished yet
    ) {}

    public record SessionResult(
            Long sessionId,
            DuelEventMessage.ParticipantResult r1,
            DuelEventMessage.ParticipantResult r2
    ) {}

    public record DisconnectResult(
            Long sessionId,
            String opponentEmail,
            DuelEventMessage eventForOpponent
    ) {}

    // ─── Progress ────────────────────────────────────────────────────────────

    @Transactional
    public Optional<ProgressResult> saveProgress(String username, DuelProgressMessage msg) {
        User user = getUser(username);
        DuelSession session = getSession(msg.getSessionId());

        participantRepository.findBySessionIdAndUserId(session.getId(), user.getId())
                .ifPresent(p -> {
                    if (p.isFinished()) return; // already finished — do not overwrite score/durationMs
                    p.setProgress(msg.getProgress());
                    p.setErrors(msg.getErrors());
                    participantRepository.save(p);
                });

        return getOpponent(session, user.getId()).map(opponent -> {
            DuelEventMessage event = DuelEventMessage.builder()
                    .type("OPPONENT_PROGRESS")
                    .opponentName(user.getUsername())
                    .opponentProgress(msg.getProgress())
                    .totalCells(session.getGridSize() * session.getGridSize())
                    .build();
            return new ProgressResult(opponent.getUser().getEmail(), event);
        });
    }

    // ─── Finish ──────────────────────────────────────────────────────────────

    @Transactional
    public FinishResult saveFinish(String username, DuelFinishMessage msg) {
        User user = getUser(username);
        DuelSession session = getSession(msg.getSessionId());

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

        // Build OPPONENT_FINISHED event for the other player
        String opponentEmail = null;
        DuelEventMessage finishedEvent = null;
        Optional<DuelParticipant> opponentOpt = getOpponent(session, user.getId());
        if (opponentOpt.isPresent()) {
            opponentEmail = opponentOpt.get().getUser().getEmail();
            finishedEvent = DuelEventMessage.builder()
                    .type("OPPONENT_FINISHED")
                    .opponentName(user.getUsername())
                    .opponentDurationMs(msg.getDurationMs())
                    .opponentErrors(msg.getErrors())
                    .opponentScore(msg.getScore())
                    .opponentProgress(msg.getProgress())
                    .build();
        }

        // Check if both finished — read fresh state after save
        List<DuelParticipant> participants = participantRepository.findAllBySessionId(session.getId());
        boolean allDone = participants.size() == 2
                && participants.stream().allMatch(p -> p.isFinished() || p.isDisconnected());

        SessionResult sessionResult = null;
        if (allDone) {
            session.setStatus("FINISHED");
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);

            DuelParticipant p1 = participants.get(0);
            DuelParticipant p2 = participants.get(1);
            sessionResult = new SessionResult(
                    session.getId(),
                    toResult(p1, session),
                    toResult(p2, session)
            );
        }

        return new FinishResult(opponentEmail, finishedEvent, sessionResult);
    }

    // ─── Leave ───────────────────────────────────────────────────────────────

    @Transactional
    public Optional<String> getOpponentEmailForLeave(String username, Long sessionId) {
        User user = getUser(username);
        DuelSession session = getSession(sessionId);
        return getOpponent(session, user.getId())
                .map(p -> p.getUser().getEmail());
    }

    // ─── Disconnect ──────────────────────────────────────────────────────────

    @Transactional
    public List<DisconnectResult> saveDisconnect(String username) {
        User user = getUser(username);

        return participantRepository.findAllByUserId(user.getId())
                .stream()
                .filter(p -> !p.isFinished())
                .filter(p -> {
                    String status = p.getSession().getStatus();
                    return "ACTIVE".equals(status) || "COUNTDOWN".equals(status);
                })
                .map(p -> {
                    p.setDisconnected(true);
                    participantRepository.save(p);

                    DuelEventMessage event = DuelEventMessage.builder()
                            .type("OPPONENT_DISCONNECTED")
                            .opponentName(user.getUsername())
                            .build();

                    // Find opponent's email
                    String opponentEmail = getOpponent(p.getSession(), user.getId())
                            .map(opp -> opp.getUser().getEmail())
                            .orElse(null);

                    return new DisconnectResult(p.getSession().getId(), opponentEmail, event);
                })
                .toList();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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

    private Optional<DuelParticipant> getOpponent(DuelSession session, Long userId) {
        return participantRepository.findAllBySessionId(session.getId())
                .stream()
                .filter(p -> !p.getUser().getId().equals(userId))
                .findFirst();
    }

    private User getUser(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private DuelSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
    }
}

