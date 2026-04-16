package com.iyanc.javarush.readsprinterback.service;

import com.iyanc.javarush.readsprinterback.dto.request.DuelFinishMessage;
import com.iyanc.javarush.readsprinterback.dto.request.DuelLeaveMessage;
import com.iyanc.javarush.readsprinterback.dto.request.DuelProgressMessage;
import com.iyanc.javarush.readsprinterback.dto.response.DuelEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DuelService}.
 * All dependencies are replaced with Mockito mocks — no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DuelService")
class DuelServiceTest {

    @Mock private DuelDbService        duelDbService;
    @Mock private MatchmakingService   matchmakingService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private DuelService duelService;

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    private static final String USERNAME       = "player@example.com";
    private static final String OPPONENT_EMAIL = "opponent@example.com";
    private static final Long   SESSION_ID     = 42L;

    private DuelProgressMessage progressMsg;
    private DuelFinishMessage   finishMsg;
    private DuelLeaveMessage    leaveMsg;

    @BeforeEach
    void setUp() {
        progressMsg = new DuelProgressMessage();
        progressMsg.setSessionId(SESSION_ID);
        progressMsg.setProgress(5);
        progressMsg.setErrors(1);

        finishMsg = new DuelFinishMessage();
        finishMsg.setSessionId(SESSION_ID);
        finishMsg.setDurationMs(12_000L);
        finishMsg.setErrors(2);
        finishMsg.setScore(800);
        finishMsg.setProgress(10);

        leaveMsg = new DuelLeaveMessage();
        leaveMsg.setSessionId(SESSION_ID);
    }

    // ─── 7.1 handleProgress — суперник є → convertAndSendToUser викликається ──

    @Test
    @DisplayName("7.1 handleProgress — saveProgress повертає результат з opponentEmail → convertAndSendToUser викликається")
    void handleProgress_withOpponentEmail_sendsToOpponent() {
        DuelEventMessage event = DuelEventMessage.builder().type("OPPONENT_PROGRESS").build();
        DuelDbService.ProgressResult result = new DuelDbService.ProgressResult(OPPONENT_EMAIL, event);
        when(duelDbService.saveProgress(USERNAME, progressMsg)).thenReturn(Optional.of(result));

        duelService.handleProgress(USERNAME, progressMsg);

        verify(messagingTemplate).convertAndSendToUser(
                eq(OPPONENT_EMAIL), eq("/queue/duel"), eq(event));
    }

    // ─── 7.2 handleProgress — opponentEmail == null → convertAndSendToUser НЕ викликається

    @Test
    @DisplayName("7.2 handleProgress — opponentEmail=null → convertAndSendToUser НЕ викликається")
    void handleProgress_nullOpponentEmail_doesNotSend() {
        DuelEventMessage event = DuelEventMessage.builder().type("OPPONENT_PROGRESS").build();
        DuelDbService.ProgressResult result = new DuelDbService.ProgressResult(null, event);
        when(duelDbService.saveProgress(USERNAME, progressMsg)).thenReturn(Optional.of(result));

        duelService.handleProgress(USERNAME, progressMsg);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    // ─── 7.3 handleFinish — суперник не фінішував (sessionResult=null) → broadcast НЕ викликається

    @Test
    @DisplayName("7.3 handleFinish — sessionResult=null → broadcast НЕ викликається")
    void handleFinish_sessionResultNull_doesNotBroadcast() {
        DuelEventMessage opponentFinishedEvent =
                DuelEventMessage.builder().type("OPPONENT_FINISHED").build();
        DuelDbService.FinishResult result =
                new DuelDbService.FinishResult(OPPONENT_EMAIL, opponentFinishedEvent, null);
        when(duelDbService.saveFinish(USERNAME, finishMsg)).thenReturn(result);

        duelService.handleFinish(USERNAME, finishMsg);

        verify(matchmakingService, never()).broadcast(any(), any());
    }

    // ─── 7.4 handleFinish — обидва фінішували → broadcast з типом SESSION_RESULT ─

    @Test
    @DisplayName("7.4 handleFinish — sessionResult != null → broadcast викликається з типом SESSION_RESULT")
    void handleFinish_bothFinished_broadcastsSessionResult() {
        DuelEventMessage.ParticipantResult r1 = DuelEventMessage.ParticipantResult.builder()
                .username(USERNAME).build();
        DuelEventMessage.ParticipantResult r2 = DuelEventMessage.ParticipantResult.builder()
                .username("other").build();
        DuelDbService.SessionResult sessionResult =
                new DuelDbService.SessionResult(SESSION_ID, r1, r2);

        DuelDbService.FinishResult result =
                new DuelDbService.FinishResult(OPPONENT_EMAIL,
                        DuelEventMessage.builder().type("OPPONENT_FINISHED").build(),
                        sessionResult);
        when(duelDbService.saveFinish(USERNAME, finishMsg)).thenReturn(result);

        duelService.handleFinish(USERNAME, finishMsg);

        ArgumentCaptor<DuelEventMessage> captor = ArgumentCaptor.forClass(DuelEventMessage.class);
        verify(matchmakingService).broadcast(eq(SESSION_ID), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("SESSION_RESULT");
    }

    // ─── 7.5 handleFinish — opponentEmail != null → convertAndSendToUser з OPPONENT_FINISHED ─

    @Test
    @DisplayName("7.5 handleFinish — opponentEmail != null → convertAndSendToUser викликається з типом OPPONENT_FINISHED")
    void handleFinish_withOpponentEmail_sendsOpponentFinished() {
        DuelEventMessage opponentFinishedEvent =
                DuelEventMessage.builder().type("OPPONENT_FINISHED").build();
        DuelDbService.FinishResult result =
                new DuelDbService.FinishResult(OPPONENT_EMAIL, opponentFinishedEvent, null);
        when(duelDbService.saveFinish(USERNAME, finishMsg)).thenReturn(result);

        duelService.handleFinish(USERNAME, finishMsg);

        ArgumentCaptor<DuelEventMessage> captor = ArgumentCaptor.forClass(DuelEventMessage.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(OPPONENT_EMAIL), eq("/queue/duel"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("OPPONENT_FINISHED");
    }

    // ─── 7.6 handleLeave — суперник знайдений → convertAndSendToUser з OPPONENT_LEFT ─

    @Test
    @DisplayName("7.6 handleLeave — суперник знайдений → convertAndSendToUser викликається з типом OPPONENT_LEFT")
    void handleLeave_opponentFound_sendsOpponentLeft() {
        when(duelDbService.getOpponentEmailForLeave(USERNAME, SESSION_ID))
                .thenReturn(Optional.of(OPPONENT_EMAIL));

        duelService.handleLeave(USERNAME, leaveMsg);

        ArgumentCaptor<DuelEventMessage> captor = ArgumentCaptor.forClass(DuelEventMessage.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(OPPONENT_EMAIL), eq("/queue/duel"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("OPPONENT_LEFT");
    }

    // ─── 7.7 handleLeave — суперника немає → convertAndSendToUser НЕ викликається ─

    @Test
    @DisplayName("7.7 handleLeave — суперник не знайдений → convertAndSendToUser НЕ викликається")
    void handleLeave_noOpponent_doesNotSend() {
        when(duelDbService.getOpponentEmailForLeave(USERNAME, SESSION_ID))
                .thenReturn(Optional.empty());

        duelService.handleLeave(USERNAME, leaveMsg);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    // ─── 7.8 handleDisconnect — активні сесії → convertAndSendToUser з OPPONENT_DISCONNECTED ─

    @Test
    @DisplayName("7.8 handleDisconnect — є активні сесії → convertAndSendToUser з типом OPPONENT_DISCONNECTED для кожної")
    void handleDisconnect_activeSessions_sendsOpponentDisconnected() {
        DuelEventMessage event1 = DuelEventMessage.builder().type("OPPONENT_DISCONNECTED").build();
        DuelEventMessage event2 = DuelEventMessage.builder().type("OPPONENT_DISCONNECTED").build();

        String opponentEmail2 = "other@example.com";

        DuelDbService.DisconnectResult result1 =
                new DuelDbService.DisconnectResult(SESSION_ID, OPPONENT_EMAIL, event1);
        DuelDbService.DisconnectResult result2 =
                new DuelDbService.DisconnectResult(99L, opponentEmail2, event2);

        when(duelDbService.saveDisconnect(USERNAME)).thenReturn(List.of(result1, result2));

        duelService.handleDisconnect(USERNAME);

        verify(messagingTemplate).convertAndSendToUser(
                eq(OPPONENT_EMAIL), eq("/queue/duel"), eq(event1));
        verify(messagingTemplate).convertAndSendToUser(
                eq(opponentEmail2), eq("/queue/duel"), eq(event2));
    }
}

