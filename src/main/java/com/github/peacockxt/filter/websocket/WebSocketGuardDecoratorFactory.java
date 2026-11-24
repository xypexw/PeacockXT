package com.github.peacockxt.filter.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketGuardDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private static final long WINDOW_MS = 15_000L;
    private static final int MAX_HEARTBEATS_PER_WINDOW = 4;
    private static final int MAX_HEARTBEAT_BYTES = 4; // Only "\n" or "\r\n" are valid

    private final WebSocketSessionRegistry sessionRegistry;
    private final ConcurrentHashMap<String, HeartbeatWindow> heartbeatWindows = new ConcurrentHashMap<>();

    @Override
    public @NotNull WebSocketHandler decorate(@NotNull WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
                sessionRegistry.register(session);
                heartbeatWindows.put(session.getId(), new HeartbeatWindow());
                super.afterConnectionEstablished(session);
            }

            @Override
            public void handleMessage(@NotNull WebSocketSession session, @NotNull WebSocketMessage<?> message)
                    throws Exception {
                if (message instanceof TextMessage textMessage) {
                    String payload = textMessage.getPayload();
                    if (isHeartbeatPayload(payload)) {
                        if (payload.length() > MAX_HEARTBEAT_BYTES || isFlooding(session.getId())) {
                            log.warn("Heartbeat flood detected [session={}]. Killing connection.", session.getId());
                            heartbeatWindows.remove(session.getId());
                            sessionRegistry.killSession(session.getId(), CloseStatus.POLICY_VIOLATION);
                            return; // Drop frame immediately, do not forward to the STOMP decoder
                        }
                    }
                }
                super.handleMessage(session, message);
            }

            @Override
            public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus closeStatus)
                    throws Exception {
                sessionRegistry.unregister(session.getId());
                heartbeatWindows.remove(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }

            @Override
            public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception)
                    throws Exception {
                sessionRegistry.unregister(session.getId());
                heartbeatWindows.remove(session.getId());
                super.handleTransportError(session, exception);
            }
        };
    }

    private boolean isHeartbeatPayload(String payload) {
        if (payload.isEmpty()) {
            return true;
        }
        for (int i = 0; i < payload.length(); i++) {
            char c = payload.charAt(i);
            if (c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }

    private boolean isFlooding(String sessionId) {
        HeartbeatWindow window = heartbeatWindows.computeIfAbsent(sessionId, k -> new HeartbeatWindow());
        return !window.tryAcquire();
    }

    private static class HeartbeatWindow {
        private long windowStartMs = System.currentTimeMillis();
        private int count = 0;

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStartMs >= WINDOW_MS) {
                windowStartMs = now;
                count = 1;
                return true;
            }
            count++;
            return count <= MAX_HEARTBEATS_PER_WINDOW;
        }
    }
}