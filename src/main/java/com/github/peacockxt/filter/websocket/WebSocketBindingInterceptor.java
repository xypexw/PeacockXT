package com.github.peacockxt.filter.websocket;

import com.github.peacockxt.infrastructure.messagequeue.DynamicBindingInstaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketBindingInterceptor implements ChannelInterceptor {

    private static final Set<StompCommand> ALLOWED_COMMANDS = EnumSet.of(
            StompCommand.CONNECT,
            StompCommand.STOMP,
            StompCommand.SUBSCRIBE,
            StompCommand.UNSUBSCRIBE,
            StompCommand.DISCONNECT
    );

    private final DynamicBindingInstaller dynamicBindingInstaller;
    private final WebSocketSessionRegistry sessionRegistry;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> sessionSubscriptions =
            new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (!ALLOWED_COMMANDS.contains(command)) {
            handleDisallowedCommand(accessor, command);
            return null; // Discard frame, do not forward to the Broker
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            if (!handleSubscribe(accessor)) {
                return null; // Discard frame, do not forward to the Broker
            }
        } else if (StompCommand.UNSUBSCRIBE.equals(command)) {
            handleUnsubscribe(accessor);
        }
        return message;
    }

    private void handleDisallowedCommand(StompHeaderAccessor accessor, StompCommand command) {
        String sessionId = accessor.getSessionId();
        SessionContext context = extractContext(accessor);

        log.warn("Disallowed STOMP command {} in Server-Push Only mode [session={}, userId={}, destination={}]. Killing connection.",
                command, sessionId, context != null ? context.getUserId() : "null", accessor.getDestination());

        if (sessionId != null) {
            sessionRegistry.killSession(sessionId, CloseStatus.POLICY_VIOLATION);
        }
    }

    /**
     * @return true if valid, false if violated (connection killed)
     */
    private boolean handleSubscribe(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        String destination = accessor.getDestination();
        SessionContext context = extractContext(accessor);

        // Destination does not match the one assigned during handshake or missing info -> Kill connection immediately
        if (sessionId == null || subscriptionId == null
                || context == null || !Objects.equals(context.getDestination(), destination)) {
            log.warn("Unauthorized SUBSCRIBE [session={}, userId={}, expected={}, actual={}]. Killing connection.",
                    sessionId,
                    context != null ? context.getUserId() : "null",
                    context != null ? context.getDestination() : "null",
                    destination);
            if (sessionId != null) {
                sessionRegistry.killSession(sessionId, CloseStatus.POLICY_VIOLATION);
            }
            return false;
        }

        ConcurrentHashMap<String, String> subs =
                sessionSubscriptions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());

        if (subs.putIfAbsent(subscriptionId, destination) == null) {
            try {
                log.debug("Client [session={}, subId={}] subscribing to: {}", sessionId, subscriptionId, destination);
                dynamicBindingInstaller.subscribeToRoutingKey(destination);
            } catch (Exception e) {
                subs.remove(subscriptionId, destination);
                throw e;
            }
        }
        return true;
    }

    private SessionContext extractContext(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UserHandshakeHandler.StompPrincipal(SessionContext context)) {
            return context;
        }
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null && attrs.get(WsTicketHandshakeInterceptor.SESSION_CONTEXT_ATTR) instanceof SessionContext ctx) {
            return ctx;
        }
        return null;
    }

    private void handleUnsubscribe(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        if (sessionId == null || subscriptionId == null) {
            return;
        }

        ConcurrentHashMap<String, String> subs = sessionSubscriptions.get(sessionId);
        if (subs != null) {
            String destination = subs.remove(subscriptionId);
            if (destination != null) {
                log.debug("Client [session={}, subId={}] unsubscribing from: {}", sessionId, subscriptionId, destination);
                dynamicBindingInstaller.unsubscribeFromRoutingKey(destination);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        ConcurrentHashMap<String, String> remainingSubs = sessionSubscriptions.remove(sessionId);

        if (remainingSubs != null && !remainingSubs.isEmpty()) {
            log.debug("Client [session={}] disconnected. Cleaning up {} remaining subscriptions.",
                    sessionId, remainingSubs.size());

            for (String subId : remainingSubs.keySet()) {
                String destination = remainingSubs.remove(subId);
                if (destination != null) {
                    dynamicBindingInstaller.unsubscribeFromRoutingKey(destination);
                }
            }
        }
    }
}