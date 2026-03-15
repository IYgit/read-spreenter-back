package com.iyanc.javarush.readsprinterback.controller;

import com.iyanc.javarush.readsprinterback.dto.request.JoinQueueRequest;
import com.iyanc.javarush.readsprinterback.service.MatchmakingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/duels")
@RequiredArgsConstructor
@Tag(name = "Duels", description = "Matchmaking and duel session management")
@SecurityRequirement(name = "bearerAuth")
public class DuelController {

    private final MatchmakingService matchmakingService;

    @PostMapping("/queue")
    @Operation(summary = "Join matchmaking queue",
            description = "Joins the queue. If an opponent is found immediately, returns matched status and sessionId. " +
                          "Otherwise returns waiting status — listen to /user/queue/duel via WebSocket for MATCH_FOUND.")
    public ResponseEntity<Map<String, Object>> joinQueue(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JoinQueueRequest request) {
        Map<String, Object> result = matchmakingService.joinQueue(userDetails.getUsername(), request);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/queue")
    @Operation(summary = "Leave matchmaking queue")
    public ResponseEntity<Void> leaveQueue(@AuthenticationPrincipal UserDetails userDetails) {
        matchmakingService.leaveQueue(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}

