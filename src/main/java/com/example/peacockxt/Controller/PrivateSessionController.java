package com.example.peacockxt.Controller;

import com.example.peacockxt.Annotation.Idempotent;
import com.example.peacockxt.DTO.Request.CreateRequest.CreatePrivateSessionRequest;
import com.example.peacockxt.DTO.Respone.PrivateSessionResponse;
import com.example.peacockxt.Service.PrivateSession.CreatePrivateSession;
import com.example.peacockxt.Service.PrivateSession.ReadPrivateSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PrivateSessionController {

    private final CreatePrivateSession createPrivateSession;
    private final ReadPrivateSession readPrivateSession;

    // Request DTOs


    // 1. READ APIs (GET)

    // 1.1 Retrieve all private sessions for the authenticated user
    // Endpoint: GET /api/v1/users/me/private-sessions
    @GetMapping("/users/me/private-sessions")
    public ResponseEntity<List<PrivateSessionResponse>> getMyPrivateSessions(
            @RequestHeader("X-User-Id") Long userId
    ) {
        long startTime = System.currentTimeMillis();
        List<PrivateSessionResponse> responses = readPrivateSession.getPrivateSessionByUser(userId);
        log.debug("Fetched user private sessions in {} ms", (System.currentTimeMillis() - startTime));
        return ResponseEntity.ok(responses);
    }

    // 1.2 Retrieve a specific private session with a target user
    // Endpoint: GET /api/v1/users/me/private-sessions/with/{targetUserId}
    @GetMapping("/users/me/private-sessions/with/{targetUserId}")
    public ResponseEntity<PrivateSessionResponse> getPrivateSessionWithUser(
            @PathVariable Long targetUserId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        long startTime = System.currentTimeMillis();
        PrivateSessionResponse response = readPrivateSession.getSinglePrivateSessionByUser(userId, targetUserId);
        log.debug("Fetched single private session in {} ms", (System.currentTimeMillis() - startTime));
        return ResponseEntity.ok(response);
    }

    // 2. WRITE APIs (POST)

    // 2.1 Create a new private session with another user
    // Endpoint: POST /api/v1/private-sessions
    @Idempotent
    @PostMapping("/private-sessions")
    public ResponseEntity<Void> createPrivateSession(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-User-Id") Long connectorId, // The user initiating the session
            @RequestBody CreatePrivateSessionRequest request
    ) {
        long startTime = System.currentTimeMillis();

        // Receiver is the target user, connector is the current logged-in user
        createPrivateSession.createPrivateSession(request.targetUserId(), connectorId);

        log.info("Created private session between {} and {} in {} ms",
                connectorId, request.targetUserId(), (System.currentTimeMillis() - startTime));

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}