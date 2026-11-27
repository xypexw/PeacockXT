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
public class CacheMembership {
    private Long membershipId;
    private Long userId;
    private Long teamId;
    private String role;
    private LocalDateTime joinedAt;
}
