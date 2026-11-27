package com.github.peacockxt.infrastructure.cache;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * Utility for batch-executing Redis commands via pipelining to minimize Round-Trip Time (RTT).
 * Commands are split into chunks of 250 ({@code CHUNK_SIZE}) to prevent client-side
 * memory pressure and avoid blocking Redis's single-threaded event loop.
 * Note: Cross-slot pipelining on Redis Cluster is not supported with Jedis (all keys must
 * share the same hash slot/node); use Lettuce instead for multi-node batches.
 */
@Component
public class RedisBatch {

    private static final int CHUNK_SIZE = 250;
    private static final long FALLBACK_TTL_SECONDS = 3600L;

    public <K, V> void multiSetWithTtl(RedisTemplate<K, V> template, Map<K, V> map, Duration ttl) {
        if (map == null || map.isEmpty()) return;
        long ttlSeconds = (ttl == null || ttl.isZero() || ttl.isNegative())
                ? FALLBACK_TTL_SECONDS
                : ttl.getSeconds();

        @SuppressWarnings("unchecked")
        RedisSerializer<K> keySerializer = (RedisSerializer<K>) template.getKeySerializer();
        @SuppressWarnings("unchecked")
        RedisSerializer<V> valueSerializer = (RedisSerializer<V>) template.getValueSerializer();

        List<Map.Entry<K, V>> entries = new ArrayList<>(map.entrySet());
        for (int start = 0; start < entries.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, entries.size());
            List<Map.Entry<K, V>> chunk = entries.subList(start, end);
            template.executePipelined((RedisCallback<Object>) connection -> {
                for (Map.Entry<K, V> entry : chunk) {
                    byte[] rawKey = keySerializer.serialize(entry.getKey());
                    byte[] rawValue = valueSerializer.serialize(entry.getValue());

                    if (rawKey != null && rawValue != null) {
                        connection.stringCommands().setEx(rawKey, ttlSeconds, rawValue);
                    }
                }
                return null;
            });
        }
    }

    public <K, V> void multiSetRemove(RedisTemplate<K, V> template, List<K> keys, V valueToRemove) {
        if (keys == null || keys.isEmpty() || valueToRemove == null) return;
        @SuppressWarnings("unchecked")
        RedisSerializer<K> keySerializer = (RedisSerializer<K>) template.getKeySerializer();
        @SuppressWarnings("unchecked")
        RedisSerializer<V> valueSerializer = (RedisSerializer<V>) template.getValueSerializer();

        byte[] rawValue = valueSerializer.serialize(valueToRemove);
        if (rawValue == null) return;

        for (int start = 0; start < keys.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, keys.size());
            List<K> chunk = keys.subList(start, end);
            template.executePipelined((RedisCallback<Object>) connection -> {
                for (K key : chunk) {
                    byte[] rawKey = keySerializer.serialize(key);
                    if (rawKey != null) {
                        connection.setCommands().sRem(rawKey, rawValue);
                    }
                }
                return null;
            });
        }
    }

    public <K, V> void multiSetRemovePairs(RedisTemplate<K, V> template, Map<K, V> keyToValueToRemoveMap) {
        if (keyToValueToRemoveMap == null || keyToValueToRemoveMap.isEmpty()) return;

        @SuppressWarnings("unchecked")
        RedisSerializer<K> keySerializer = (RedisSerializer<K>) template.getKeySerializer();
        @SuppressWarnings("unchecked")
        RedisSerializer<V> valueSerializer = (RedisSerializer<V>) template.getValueSerializer();

        List<Map.Entry<K, V>> entries = new ArrayList<>(keyToValueToRemoveMap.entrySet());
        for (int start = 0; start < entries.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, entries.size());
            List<Map.Entry<K, V>> chunk = entries.subList(start, end);
            template.executePipelined((RedisCallback<Object>) connection -> {
                for (Map.Entry<K, V> entry : chunk) {
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
}
