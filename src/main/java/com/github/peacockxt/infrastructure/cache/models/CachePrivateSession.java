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
public class CachePrivateSession {
    private Long id;
    private Long connectorId;
    private Long receiverId;
    private LocalDateTime createAt;
    private String status;
    private Long createBy;
}
