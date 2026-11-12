package com.github.peacockxt.controller;

import com.github.peacockxt.annotation.Idempotent;
import com.github.peacockxt.dto.response.MessageResponse;
import com.github.peacockxt.service.message.CreateMessageService;
import com.github.peacockxt.service.message.DeleteMessageService;
import com.github.peacockxt.service.message.EditMessageService;
import com.github.peacockxt.service.message.ReadMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channels/{channelId}/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final ReadMessageService readMessageService;
    private final CreateMessageService createMessageService;
    private final EditMessageService editMessageService;
    private final DeleteMessageService deleteMessageService;

    // DTO Requests
    public record CreateMessageRequest(String content, Long replyId) {}
    public record EditMessageRequest(String content) {}

    // --- GET: Retrieve message list ---
    @GetMapping
    public ResponseEntity<List<MessageResponse>> readMessages(
            @PathVariable Long channelId,
            @RequestParam(required = false) Long cursorId,
            @RequestHeader("X-Membership-Id") Long membershipId
    ) {
        long startTime = System.currentTimeMillis();
        List<MessageResponse> responses = readMessageService.readMessages(channelId, cursorId, membershipId);
        log.debug("Read messages took {} ms", (System.currentTimeMillis() - startTime));

        return ResponseEntity.ok(responses);
    }

    // --- POST: Create a new message ---
    @Idempotent
    @PostMapping
    public ResponseEntity<Void> createMessage(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long channelId,
            @RequestHeader("X-User-Id") Long senderId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @RequestBody CreateMessageRequest request
    ) {
        createMessageService.createMessage(
                senderId,
                request.content(),
                request.replyId(),
                channelId,
                membershipId
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // --- PUT: Edit message ---
    @Idempotent
    @PutMapping("/{messageId}")
    public ResponseEntity<Void> editMessage(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @RequestBody EditMessageRequest request
    ) {
        // Future: update editMessageService to check edit permissions if needed
        editMessageService.editMessage(messageId, request.content(), channelId);
        return ResponseEntity.ok().build();
    }

    // --- DELETE: Delete message ---
    @Idempotent
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long channelId,
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId
    ) {
        // Future: update deleteMessageService to check delete permissions if needed
        deleteMessageService.deleteMessage(channelId, messageId);
        return ResponseEntity.ok().build();
    }
}