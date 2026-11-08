package com.example.peacockxt.Infrastructure.Cache;

import com.example.peacockxt.Models.CustomEmoji;
import com.example.peacockxt.Infrastructure.Constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmojiRedisDao {

    private final RedisTemplate<String, CustomEmoji> customEmojiRedisTemplate;
    private final RedisOperationGuard redisGuard;

    public void saveEmoji(Long teamId, CustomEmoji emoji) {
        redisGuard.safeWrite(() -> { String key = RedisKey.TEAM_EMOJIS.build(teamId);
                    customEmojiRedisTemplate.<String, CustomEmoji>opsForHash().put(key, emoji.getEmojiName(), emoji);},
                "saveEmoji [" + emoji.getEmojiName() + "] for Team: " + teamId);
    }

    public CustomEmoji getEmoji(Long teamId, String emojiName) {
        return redisGuard.safeRead(() -> { String key = RedisKey.TEAM_EMOJIS.build(teamId);
                    return customEmojiRedisTemplate.<String, CustomEmoji>opsForHash().get(key, emojiName);
                }, null, "getEmoji [" + emojiName + "] for Team: " + teamId
        );
    }

    public Map<String, CustomEmoji> getAllEmojis(Long teamId) {
        return redisGuard.safeRead(() -> {String key = RedisKey.TEAM_EMOJIS.build(teamId);
                    return customEmojiRedisTemplate.<String, CustomEmoji>opsForHash().entries(key);
                },
                Collections.emptyMap(), "getAllEmojis for Team: " + teamId
        );
    }

    public void setAllEmoji(Long teamId, List<CustomEmoji> emojis) {
        if (emojis == null || emojis.isEmpty()) return;

        redisGuard.safeWrite(() -> {
            String key = RedisKey.TEAM_EMOJIS.build(teamId);
            Map<String, CustomEmoji> map = new HashMap<>();
            for (CustomEmoji emoji : emojis) {
                map.put(emoji.getEmojiName(), emoji);
            }
            customEmojiRedisTemplate.opsForHash().putAll(key, map);
            },
                "setAllEmoji (" + emojis.size() + " emojis) for Team: " + teamId
        );
    }

    public void deleteEmoji(Long teamId, String emojiName) {
        redisGuard.safeWrite(() -> {
                    String key = RedisKey.TEAM_EMOJIS.build(teamId);
                    customEmojiRedisTemplate.<String, CustomEmoji>opsForHash().delete(key, emojiName);
                    }, "deleteEmoji [" + emojiName + "] for Team: " + teamId );
    }
}