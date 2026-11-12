package com.github.peacockxt.controller;

import com.github.peacockxt.annotation.Idempotent;
import com.github.peacockxt.dto.request.createrequest_temp.CreateEmojiRequest;
import com.github.peacockxt.dto.response.EmojiResponse;
import com.github.peacockxt.service.storage.EmojiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/emojis")
@RequiredArgsConstructor
public class EmojiController {

    private final EmojiService emojiService;

    @Idempotent
    @PostMapping
    public ResponseEntity<Void> createEmoji(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateEmojiRequest request
    ) {
        emojiService.createEmoji(
                teamId,
                userId,
                request.getEmojiName(),
                request.getObjectKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Idempotent
    @DeleteMapping("/{emojiName}")
    public ResponseEntity<Void> deleteEmoji(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @PathVariable String emojiName,
            @RequestHeader("X-User-Id") Long userId
    ) {
        emojiService.deleteEmoji(teamId, userId, emojiName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<EmojiResponse>> getAllEmojis(
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        List<EmojiResponse> responses = emojiService.getAllCustomEmojisOfTeam(teamId, userId);
        return ResponseEntity.ok(responses);
    }
}