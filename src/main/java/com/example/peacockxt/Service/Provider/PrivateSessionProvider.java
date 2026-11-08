package com.example.peacockxt.Service.Provider;

import com.example.peacockxt.Infrastructure.Cache.PrivateSessionRedisDao;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.PrivateSession;
import com.example.peacockxt.Repository.Imlpementation.PrivateSessionRepository;
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

    public PrivateSession getPrivateSession(Long sessionId) {
        if (sessionId == null) {
            return null;
        }

        // 1. Quét Cache
        List<PrivateSession> cachedSessions = privateSessionRedisDao.getSessionBatch(Collections.singletonList(sessionId));
        if (cachedSessions != null && !cachedSessions.isEmpty() && cachedSessions.get(0) != null) {
            return cachedSessions.get(0);
        }
        // 2. Cache Miss -> Lội DB
        try {
            PrivateSession privateSession = privateSessionRepository.getPrivateSessionById(sessionId);
            if (privateSession != null) {
                privateSessionRedisDao.saveSessionInfo(sessionId, privateSession);
            }
            return privateSession;
        } catch (Exception e) {
            log.error("Error querying PrivateSession from DB for id: {}", sessionId, e);
            throw new DatabaseException("Failed to query PrivateSession from database for sessionId: " + sessionId, e);
        }
    }
}