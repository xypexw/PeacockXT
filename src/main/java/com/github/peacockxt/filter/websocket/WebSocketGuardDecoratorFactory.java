package com.github.peacockxt.filter.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketGuardDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private static final long WINDOW_MS = 15_000L;
    private static final int MAX_HEARTBEATS_PER_WINDOW = 4;
    private static final int MAX_TOTAL_FRAMES_PER_WINDOW = 20;
    private static final int MAX_HEARTBEAT_BYTES = 4;

    private final WebSocketSessionRegistry sessionRegistry;
    private final ConcurrentHashMap<String, RateWindow> rateWindows = new ConcurrentHashMap<>();

    @Override
    public @NotNull WebSocketHandler decorate(@NotNull WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
                sessionRegistry.register(session);
                rateWindows.put(session.getId(), new RateWindow());
                super.afterConnectionEstablished(session);
            }

            @Override
            public void handleMessage(@NotNull WebSocketSession session, @NotNull WebSocketMessage<?> message)
                    throws Exception {
                String sessionId = session.getId();

                // 1. Block BinaryMessage (prevent bypassing Heartbeat Flood via binary "\n")
                if (message instanceof BinaryMessage) {
                    log.warn("Binary WebSocket frame not allowed [session={}]. Killing connection.", sessionId);
                    rateWindows.remove(sessionId);
                    sessionRegistry.killSession(sessionId, CloseStatus.NOT_ACCEPTABLE);
                    return;
                }

                // 2. Enforce total rate limit & Heartbeat Flood protection (TextMessage only)
                if (message instanceof TextMessage textMessage) {
                    String payload = textMessage.getPayload();
                    boolean isHeartbeat = isHeartbeatPayload(payload);
                    RateWindow window = rateWindows.computeIfAbsent(sessionId, k -> new RateWindow());

                    if ((isHeartbeat && payload.length() > MAX_HEARTBEAT_BYTES)
                            || !window.tryAcquire(isHeartbeat)) {
                        log.warn("Inbound frame flood detected [session={}, heartbeat={}]. Killing.",
                                sessionId, isHeartbeat);
                        rateWindows.remove(sessionId);
                        sessionRegistry.killSession(sessionId, CloseStatus.POLICY_VIOLATION);
                        return;
                    }
                }
                super.handleMessage(session, message);
            }

            @Override
            public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus closeStatus)
                    throws Exception {
                try {
                    sessionRegistry.unregister(session.getId());
                    rateWindows.remove(session.getId());
                } finally {
                    super.afterConnectionClosed(session, closeStatus);
                }
            }

            @Override
            public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception)
                    throws Exception {
                // DO NOT unregister here! SubProtocolWebSocketHandler does not automatically close the session on handleTransportError.
                // Cleanup is guaranteed to run in afterConnectionClosed once the socket actually closes.
                super.handleTransportError(session, exception);
            }
        };
    }

    private boolean isHeartbeatPayload(String payload) {
        if (payload.isEmpty()) return true;
        for (int i = 0; i < payload.length(); i++) {
            char c = payload.charAt(i);
            if (c != '\n' && c != '\r') return false;
        }
        return true;
    }

    // Use ReentrantLock instead of synchronized (best practice for Java 21 Virtual Threads)
    private static class RateWindow {
        private final ReentrantLock lock = new ReentrantLock();
        private long windowStartMs = System.currentTimeMillis();
        private int totalCount = 0;
        private int heartbeatCount = 0;

        boolean tryAcquire(boolean isHeartbeat) {
            lock.lock();
            try {
                long now = System.currentTimeMillis();
                if (now - windowStartMs >= WINDOW_MS) {
                    windowStartMs = now;
                    totalCount = 0;
                    heartbeatCount = 0;
                }
                totalCount++;
                if (totalCount > MAX_TOTAL_FRAMES_PER_WINDOW) {
                    return false;
                }
                if (isHeartbeat) {
                    heartbeatCount++;
                    return heartbeatCount <= MAX_HEARTBEATS_PER_WINDOW;
                }
                return true;
            } finally {
                lock.unlock();
            }
        }
    }
}