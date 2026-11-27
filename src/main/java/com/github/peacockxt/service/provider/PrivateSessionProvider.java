package com.github.peacockxt.service.provider;

import com.github.peacockxt.infrastructure.cache.PrivateSessionRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CachePrivateSession;
import com.github.peacockxt.models.PrivateSession;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.PrivateSessionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class PrivateSessionProvider {

    private final PrivateSessionRepository privateSessionRepository;
    private final PrivateSessionRedisDao privateSessionRedisDao;

    public boolean hasPrivateSession(Long sessionId) {
        return getPrivateSession(sessionId) != null;
    }

    public CachePrivateSession getPrivateSession(Long sessionId) {
        if (sessionId == null) {
            return null;
        }

        List<CachePrivateSession> cachedSessions = privateSessionRedisDao.getSessionBatch(Collections.singletonList(sessionId));
        if (cachedSessions != null && !cachedSessions.isEmpty() && cachedSessions.get(0) != null) {
            return cachedSessions.get(0);
        }

        try {
            PrivateSession privateSession = privateSessionRepository.getPrivateSessionById(sessionId);
            if (privateSession == null) {
                return null;
            }
            CachePrivateSession cacheSession = CachePrivateSession.builder()
                    .id(privateSession.getId())
                    .connectorId(privateSession.getConnectorId())
                    .receiverId(privateSession.getReceiverId())
                    .createAt(privateSession.getCreateAt())
                    .status(privateSession.getStatus())
                    .createBy(privateSession.getCreateBy())
                    .build();
            privateSessionRedisDao.saveSessionInfo(sessionId, cacheSession);
            return cacheSession;
        } catch (Exception e) {
            log.error("Error querying PrivateSession from DB for id: {}", sessionId, e);
            throw new DatabaseException("Failed to query PrivateSession from database for sessionId: " + sessionId, e);
        }
    }
}
