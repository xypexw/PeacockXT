package com.github.peacockxt.infrastructure.cache.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheDirectMessage {
    private Long messageId;
    private String content;
    private Long senderId;
    private String status;
    private LocalDateTime updatedAt;
    private LocalDateTime createAt;
    private LocalDateTime sendAt;
    private Long replyToId;
    private Long sessionId;
}
