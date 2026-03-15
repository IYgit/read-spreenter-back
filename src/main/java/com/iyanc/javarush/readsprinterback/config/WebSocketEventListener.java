package com.iyanc.javarush.readsprinterback.config;

import com.iyanc.javarush.readsprinterback.service.DuelService;
import com.iyanc.javarush.readsprinterback.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final MatchmakingService matchmakingService;
    private final DuelService duelService;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String username = null;
        if (event.getUser() != null) {
            username = event.getUser().getName();
        }
        if (username == null) return;

        log.info("WebSocket disconnect: user={}", username);

        // Remove from matchmaking queue if still waiting
        matchmakingService.leaveQueue(username);

        // Notify opponent in active duel if any
        duelService.handleDisconnect(username);
    }
}

