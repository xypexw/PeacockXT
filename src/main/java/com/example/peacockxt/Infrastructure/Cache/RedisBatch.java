package com.example.peacockxt.Infrastructure.Cache;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import java.util.List;
import java.time.Duration;
import java.util.Map;

@Component
public class RedisBatch {

    public <K, V> void multiSetWithTtl(RedisTemplate<K, V> template, Map<K, V> map, Duration ttl) {
        if (map == null || map.isEmpty()) return;
        @SuppressWarnings("unchecked")
        RedisSerializer<K> keySerializer = (RedisSerializer<K>) template.getKeySerializer();
        @SuppressWarnings("unchecked")
        RedisSerializer<V> valueSerializer = (RedisSerializer<V>) template.getValueSerializer();
        template.executePipelined((RedisCallback<Object>) connection -> {
            long ttlSeconds = ttl.getSeconds();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                byte[] rawKey = keySerializer.serialize(entry.getKey());
                byte[] rawValue = valueSerializer.serialize(entry.getValue());

                if (rawKey != null && rawValue != null) {
                    connection.stringCommands().setEx(rawKey, ttlSeconds, rawValue);
                }
            }
            return null;
        });
    }


    public <K, V> void multiSetRemove(RedisTemplate<K, V> template, List<K> keys, V valueToRemove) {
        if (keys == null || keys.isEmpty()) return;
        @SuppressWarnings("unchecked")
        RedisSerializer<K> keySerializer = (RedisSerializer<K>) template.getKeySerializer();
        @SuppressWarnings("unchecked")
        RedisSerializer<V> valueSerializer = (RedisSerializer<V>) template.getValueSerializer();
        template.executePipelined((RedisCallback<Object>) connection -> {
            byte[] rawValue = valueSerializer.serialize(valueToRemove);
            if (rawValue == null) return null;

            for (K key : keys) {
                byte[] rawKey = keySerializer.serialize(key);
                if (rawKey != null) {
                    connection.setCommands().sRem(rawKey, rawValue);
                }
            }
            return null;
        });
    }

    public <K, V> void multiSetRemovePairs(RedisTemplate<K, V> template, Map<K, V> keyToValueToRemoveMap) {
        if (keyToValueToRemoveMap == null || keyToValueToRemoveMap.isEmpty()) return;

        @SuppressWarnings("unchecked")
        RedisSerializer<K> keySerializer = (RedisSerializer<K>) template.getKeySerializer();
        @SuppressWarnings("unchecked")
        RedisSerializer<V> valueSerializer = (RedisSerializer<V>) template.getValueSerializer();

        template.executePipelined((RedisCallback<Object>) connection -> {
            for (Map.Entry<K, V> entry : keyToValueToRemoveMap.entrySet()) {
                byte[] rawKey = keySerializer.serialize(entry.getKey());
                byte[] rawValue = valueSerializer.serialize(entry.getValue());

                if (rawKey != null && rawValue != null) {
                    connection.setCommands().sRem(rawKey, rawValue);
                }
            }
            return null;
        });
    }

}