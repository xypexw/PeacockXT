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
public class CacheMessage {
    private Long messageId;
    private String content;
    private Long senderId;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private String status;
    private Long replyToId;
    private Long channelId;
}
