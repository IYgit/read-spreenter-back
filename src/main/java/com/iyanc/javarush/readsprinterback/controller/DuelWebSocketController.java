package com.iyanc.javarush.readsprinterback.controller;

import com.iyanc.javarush.readsprinterback.dto.request.DuelFinishMessage;
import com.iyanc.javarush.readsprinterback.dto.request.DuelLeaveMessage;
import com.iyanc.javarush.readsprinterback.dto.request.DuelProgressMessage;
import com.iyanc.javarush.readsprinterback.service.DuelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DuelWebSocketController {

    private final DuelService duelService;

    /** Client sends: /app/duel/progress */
    @MessageMapping("/duel/progress")
    public void progress(@Payload DuelProgressMessage msg, Principal principal) {
        log.debug("Progress from {}: session={} progress={}", principal.getName(), msg.getSessionId(), msg.getProgress());
        duelService.handleProgress(principal.getName(), msg);
    }

    /** Client sends: /app/duel/finish */
    @MessageMapping("/duel/finish")
    public void finish(@Payload DuelFinishMessage msg, Principal principal) {
        log.info("Finish from {}: session={} duration={}ms", principal.getName(), msg.getSessionId(), msg.getDurationMs());
        duelService.handleFinish(principal.getName(), msg);
    }

    /** Client sends: /app/duel/leave */
    @MessageMapping("/duel/leave")
    public void leave(@Payload DuelLeaveMessage msg, Principal principal) {
        log.info("Leave from {}: session={}", principal.getName(), msg.getSessionId());
        duelService.handleLeave(principal.getName(), msg);
    }
}

