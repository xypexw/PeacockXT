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
public class CachePost {
    private Long postId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Long createBy;
    private Long updateBy;
    private Long teamId;
}
