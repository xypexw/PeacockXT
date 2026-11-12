package com.github.peacockxt.infrastructure.cache;

import com.github.peacockxt.models.PrivateSession;
import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


@Component
@RequiredArgsConstructor
public class PrivateSessionRedisDao {

    private final RedisTemplate<String,PrivateSession> privateSessionRedisTemplate;
    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RedisBatch redisBatch;

    private Duration addJitter(Duration baseTtl) {
        if (baseTtl == null) return null;
        return baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(1800));
    }

    public void saveSessionInfo(Long sessionId, PrivateSession session) {
        privateSessionRedisTemplate.opsForValue()
                .set(RedisKey.PRIVATE_SESSION_INFO.build(sessionId), session ,
                        addJitter(RedisKey.PRIVATE_SESSION_INFO.getTtl()));
    }

    public List<PrivateSession> getAllSessionInfo(List<Long> sessionIds) {
        List<String> keys = new ArrayList<>();
        for (Long id : sessionIds) {
            keys.add(RedisKey.PRIVATE_SESSION_INFO.build(id));
        }
        List<PrivateSession> result = privateSessionRedisTemplate.opsForValue().multiGet(keys);
        if(result==null || result.isEmpty())    return Collections.emptyList();
        else return result;
    }

    public void addSessionToReceiverIdAndConnectorId( Long receiverId , Long connectorId , Long sessionId ) {
        if(receiverId==null || connectorId==null || sessionId==null) return;
        longRedisTemplate.opsForZSet().add(RedisKey.USER_INBOX_INDEX.build(receiverId),
                sessionId , sessionId.doubleValue());
        longRedisTemplate.opsForZSet().add(RedisKey.USER_INBOX_INDEX.build(connectorId),
                sessionId , sessionId.doubleValue());
    }

    public Set<Long> getAllSessionInbox(Long userId) {
        if(userId==null) return Collections.emptySet() ;
        Set<Long> result = longRedisTemplate.opsForZSet().range(RedisKey.USER_INBOX_INDEX.build(userId), 0, -1);
        if(result==null || result.isEmpty()) return Collections.emptySet();
        else return result;
    }

    public List<PrivateSession> getSessionBatch(List<Long> sessionIds) {
        if(sessionIds==null || sessionIds.isEmpty()) return Collections.emptyList();
        List<String> key = new ArrayList<>();
        for (Long id : sessionIds) {
            key.add(RedisKey.PRIVATE_SESSION_INFO.build(id));
        }
        List<PrivateSession> result = privateSessionRedisTemplate.opsForValue().multiGet(key);
        if(result==null || result.isEmpty()) return Collections.emptyList();
        else return result;
    }


    public void saveAllSessionIdsToInbox(Long userId , List<Long> sessionIds) {
        if(userId==null || sessionIds==null || sessionIds.isEmpty()) return;
        String key = RedisKey.USER_INBOX_INDEX.build(userId);
        Set<ZSetOperations.TypedTuple<Long>> tuples = new HashSet<>();
        for(Long id : sessionIds){
            tuples.add(new DefaultTypedTuple<Long>(id,id.doubleValue()));
        }
        longRedisTemplate.opsForZSet().add(key,tuples);
    }

    public void saveSessionInfoBatch(List<PrivateSession> session) {
        Map<String,PrivateSession> map = new HashMap<>();
        for(PrivateSession privateSession : session){
            map.put(RedisKey.PRIVATE_SESSION_INFO.build(privateSession.getId()),privateSession);
        }
        Duration timeToLive = addJitter(RedisKey.PRIVATE_SESSION_INFO.getTtl());
        redisBatch.multiSetWithTtl(privateSessionRedisTemplate,map,timeToLive);
    }

    public void deleteSessionData(Long sessionId) {
        String key = RedisKey.PRIVATE_SESSION_INFO.build(sessionId);
        privateSessionRedisTemplate.delete(key);
    }


}