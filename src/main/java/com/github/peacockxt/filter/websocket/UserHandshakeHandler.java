package com.github.peacockxt.filter.websocket;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            @NotNull ServerHttpRequest request,
            @NotNull WebSocketHandler wsHandler,
            @NotNull Map<String, Object> attributes
    ) {
        Object attr = attributes.get(WsTicketHandshakeInterceptor.SESSION_CONTEXT_ATTR);
        if (!(attr instanceof SessionContext context)) {
            return null;
        }
        return new StompPrincipal(context);
    }

    public record StompPrincipal(SessionContext context) implements Principal {
        @Override
        public String getName() {
            return String.valueOf(context.getUserId());
        }
    }
}