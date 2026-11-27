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
public class CacheChannel {
    private Long channelId;
    private String name;
    private String description;
    private String status;
    private Long createBy;
    private Long updateBy;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Long teamId;
}
