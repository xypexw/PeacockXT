package com.github.peacockxt.controller;

import com.github.peacockxt.annotation.Idempotent;
import com.github.peacockxt.dto.request.createrequest_temp.CreateDirectMessageRequest;
import com.github.peacockxt.dto.request.updaterequest_temp.UpdateDirectMessageRequest;
import com.github.peacockxt.dto.response.DirectMessageResponse;
import com.github.peacockxt.service.directmessage.CreateDirectMessageService;
import com.github.peacockxt.service.directmessage.DeleteDirectMessageService;
import com.github.peacockxt.service.directmessage.ReadDirectMessageService;
import com.github.peacockxt.service.directmessage.UpdateDirectMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/private-sessions/{sessionId}/messages")
@RequiredArgsConstructor
@Slf4j
public class DirectMessageController {

    private final CreateDirectMessageService createDirectMessageService;
    private final ReadDirectMessageService readDirectMessageService;
    private final UpdateDirectMessageService updateDirectMessageService;
    private final DeleteDirectMessageService deleteDirectMessageService;

    // 1. READ MESSAGES (GET)
    @GetMapping
    public ResponseEntity<List<DirectMessageResponse>> readMessages(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long cursorId,
            @RequestHeader("X-User-Id") Long userId // User information forwarded from API Gateway
    ) {
        long startTime = System.currentTimeMillis();

        List<DirectMessageResponse> responses = readDirectMessageService.readDirectMessage(sessionId, cursorId, userId);

        log.debug("Read direct messages for session {} completed in {} ms", sessionId, (System.currentTimeMillis() - startTime));
        return ResponseEntity.ok(responses);
    }

    // 2. CREATE NEW MESSAGE (POST)
    @Idempotent
    @PostMapping
    public ResponseEntity<Void> createMessage(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long sessionId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CreateDirectMessageRequest request
    ) {
        createDirectMessageService.createDirectMessage(
                userId,
                request.content(),
                request.replyToId(),
                sessionId
        );
        // Successful POST creation returns 201 CREATED
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 3. UPDATE MESSAGE (PUT)
    @Idempotent
    @PutMapping("/{messageId}")
    public ResponseEntity<Void> updateMessage(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long sessionId,
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateDirectMessageRequest request
    ) {
        updateDirectMessageService.updatedDirectMessage(
                messageId,
                sessionId,
                userId,
                request.content()
        );
        return ResponseEntity.ok().build();
    }

    // 4. DELETE MESSAGE (DELETE)
    @Idempotent
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long sessionId,
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        deleteDirectMessageService.deletedDirectMessage(
                messageId,
                sessionId,
                userId
        );
        return ResponseEntity.ok().build();
    }
}