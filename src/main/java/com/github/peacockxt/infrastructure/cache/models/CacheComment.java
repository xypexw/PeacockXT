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
public class CacheComment {
    private Long commentId;
    private String content;
    private String status;
    private Long createBy;
    private Long updateBy;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Long replyToId;
    private Long postId;
}
