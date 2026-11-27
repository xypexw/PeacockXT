package com.github.peacockxt.infrastructure.cache.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheCustomEmoji {
    private Long emojiId;
    private String emojiName;
    private String imageUrl;
    private Long createBy;
    private Long teamId;
    private String status;
}
