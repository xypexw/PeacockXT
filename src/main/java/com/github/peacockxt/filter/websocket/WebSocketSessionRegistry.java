package com.github.peacockxt.filter.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionRegistry {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    public void killSession(String sessionId, CloseStatus status) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.close(status);
            } catch (IOException e) {
                log.debug("Failed to close session={} cleanly: {}", sessionId, e.getMessage());
            }
        }
    }
}