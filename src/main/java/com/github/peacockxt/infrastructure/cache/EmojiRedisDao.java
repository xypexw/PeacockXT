package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.infrastructure.cache.models.CacheCustomEmoji;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class EmojiRedisDao {

    private final RedisTemplate<String, CacheCustomEmoji> customEmojiRedisTemplate;
    private final RedisOperationGuard redisGuard;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    public void saveEmoji(Long teamId, CacheCustomEmoji emoji) {
        if (teamId == null || emoji == null || emoji.getEmojiName() == null) return;
        redisGuard.safeWrite(() -> {
            String key = RedisKey.TEAM_EMOJIS.build(teamId);
            if (Boolean.TRUE.equals(customEmojiRedisTemplate.hasKey(key))) {
                customEmojiRedisTemplate.<String, CacheCustomEmoji>opsForHash().put(key, emoji.getEmojiName(), emoji);
                customEmojiRedisTemplate.expire(key, addJitter(RedisKey.TEAM_EMOJIS.getTtl()));
            }
        }, "saveEmoji [" + emoji.getEmojiName() + "] for Team: " + teamId);
    }

    public CacheCustomEmoji getEmoji(Long teamId, String emojiName) {
        if (teamId == null || emojiName == null) return null;
        return redisGuard.safeRead(() -> {
            String key = RedisKey.TEAM_EMOJIS.build(teamId);
            return customEmojiRedisTemplate.<String, CacheCustomEmoji>opsForHash().get(key, emojiName);
        }, null, "getEmoji [" + emojiName + "] for Team: " + teamId);
    }

    public Map<String, CacheCustomEmoji> getAllEmojis(Long teamId) {
        if (teamId == null) return Collections.emptyMap();
        return redisGuard.safeRead(() -> {
            String key = RedisKey.TEAM_EMOJIS.build(teamId);
            Map<String, CacheCustomEmoji> entries = customEmojiRedisTemplate.<String, CacheCustomEmoji>opsForHash().entries(key);
            return entries == null ? Collections.emptyMap() : entries;
        }, Collections.emptyMap(), "getAllEmojis for Team: " + teamId);
    }

    public void setAllEmoji(Long teamId, List<CacheCustomEmoji> emojis) {
        if (teamId == null || emojis == null || emojis.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            String key = RedisKey.TEAM_EMOJIS.build(teamId);
            Map<String, CacheCustomEmoji> map = new HashMap<>();
            for (CacheCustomEmoji emoji : emojis) {
                if (emoji != null && emoji.getEmojiName() != null) {
                    map.put(emoji.getEmojiName(), emoji);
                }
            }
            if (!map.isEmpty()) {
                customEmojiRedisTemplate.opsForHash().putAll(key, map);
                customEmojiRedisTemplate.expire(key, addJitter(RedisKey.TEAM_EMOJIS.getTtl()));
            }
        }, "setAllEmoji (" + emojis.size() + " emojis) for Team: " + teamId);
    }

    public void deleteEmoji(Long teamId, String emojiName) {
        if (teamId == null || emojiName == null) return;
        redisGuard.safeInvalidate(() -> {
            String key = RedisKey.TEAM_EMOJIS.build(teamId);
            customEmojiRedisTemplate.<String, CacheCustomEmoji>opsForHash().delete(key, emojiName);
        }, "deleteEmoji [" + emojiName + "] for Team: " + teamId);
    }

    public void deleteAllTeamEmojis(Long teamId) {
        if (teamId == null) return;
        redisGuard.safeInvalidate(
                () -> customEmojiRedisTemplate.delete(RedisKey.TEAM_EMOJIS.build(teamId)),
                "deleteAllTeamEmojis for Team: " + teamId
        );
    }
}
