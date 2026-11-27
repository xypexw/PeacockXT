package com.github.peacockxt.service.privatesession;

import com.github.peacockxt.dto.response.PrivateSessionResponse;
import com.github.peacockxt.infrastructure.cache.PrivateSessionRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CachePrivateSession;
import com.github.peacockxt.models.PrivateSession;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.PrivateSessionRepository;
import com.github.peacockxt.service.provider.UserProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Service
@AllArgsConstructor
public class ReadPrivateSession {
    private final PrivateSessionRedisDao privateSessionRedisDao;
    private final PrivateSessionRepository privateSessionRepository;
    private final UserProvider userProvider;

    public PrivateSessionResponse getSinglePrivateSessionByUser(Long userId, Long targetId) {
        if (!userProvider.hasUser(userId))
            throw new BusinessLogicException("User does not exist");

        if (!userProvider.hasUser(targetId))
            throw new BusinessLogicException("Target User does not exist");

        Long smallerId = min(userId, targetId);
        Long largerId = max(userId, targetId);

        PrivateSession foundSession = privateSessionRepository.findByConnectorIdAndReceiverId(smallerId, largerId)
                .orElseThrow(() -> new BusinessLogicException("Private session not found"));

        privateSessionRedisDao.saveSessionInfo(foundSession.getId(), toCachePrivateSession(foundSession));

        PrivateSessionResponse privateSessionResponse = new PrivateSessionResponse();
        privateSessionResponse.setPartnerId(targetId);
        privateSessionResponse.setPrivateSessionId(foundSession.getId());
        return privateSessionResponse;
    }

    public List<PrivateSessionResponse> getPrivateSessionByUser(Long userId) {
        if (!userProvider.hasUser(userId)) throw new BusinessLogicException("User does not exist");

        List<Long> indexes = getPrivateSessionIds(userId);
        if (indexes.isEmpty()) {
            return Collections.emptyList();
        }
        List<CachePrivateSession> privateSessions = getPrivateSessionByIds(indexes);
        return privateSessionResponseBuilder(privateSessions, userId);
    }

    private List<Long> getPrivateSessionIds(Long userId) {
        Set<Long> ids = privateSessionRedisDao.getAllSessionInbox(userId);
        if (ids != null && !ids.isEmpty()) {
            return new ArrayList<>(ids);
        }
        List<Long> result = privateSessionRepository.getPrivateSessionIdsByUserId(userId);
        if (result != null && !result.isEmpty()) {
            privateSessionRedisDao.saveAllSessionIdsToInbox(userId, result);
            return result;
        }
        return Collections.emptyList();
    }

    private List<CachePrivateSession> getPrivateSessionByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<CachePrivateSession> cached = privateSessionRedisDao.getSessionBatch(ids);
        Map<Long, CachePrivateSession> map = new HashMap<>(ids.size());
        if (cached != null) {
            for (CachePrivateSession ps : cached) {
                if (ps != null) {
                    map.put(ps.getId(), ps);
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long id : ids) {
            if (!map.containsKey(id)) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<PrivateSession> fromDb = privateSessionRepository.getPrivateSessionListByIds(missingIds);
            List<CachePrivateSession> toCache = new ArrayList<>(fromDb.size());
            for (PrivateSession entity : fromDb) {
                CachePrivateSession cacheSession = toCachePrivateSession(entity);
                map.put(cacheSession.getId(), cacheSession);
                toCache.add(cacheSession);
            }
            if (!toCache.isEmpty()) {
                privateSessionRedisDao.saveSessionInfoBatch(toCache);
            }
        }

        List<CachePrivateSession> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            CachePrivateSession ps = map.get(id);
            if (ps != null) {
                result.add(ps);
            }
        }
        return result;
    }

    List<PrivateSessionResponse> privateSessionResponseBuilder(List<CachePrivateSession> privateSessions, Long userId) {
        List<PrivateSessionResponse> result = new ArrayList<>();
        for (CachePrivateSession privateSession : privateSessions) {
            if (privateSession == null) continue;
            PrivateSessionResponse privateSessionResponse = new PrivateSessionResponse();
            privateSessionResponse.setPrivateSessionId(privateSession.getId());
            if (privateSession.getConnectorId().equals(userId)) {
                privateSessionResponse.setPartnerId(privateSession.getReceiverId());
            } else {
                privateSessionResponse.setPartnerId(privateSession.getConnectorId());
            }
            result.add(privateSessionResponse);
        }
        return result;
    }

    private CachePrivateSession toCachePrivateSession(PrivateSession entity) {
        return CachePrivateSession.builder()
                .id(entity.getId())
                .connectorId(entity.getConnectorId())
                .receiverId(entity.getReceiverId())
                .createAt(entity.getCreateAt())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .build();
    }
}
