package com.github.peacockxt.filter.websocket;

import com.github.peacockxt.infrastructure.constants.RedisKey;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WsTicketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String SESSION_CONTEXT_ATTR = "WS_SESSION_CONTEXT";

    private final StringRedisTemplate redisTemplate;
    private final WebSocketSecurityService securityService;

    @Override
    public boolean beforeHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            @NotNull Map<String, Object> attributes
    ) {
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();

        String ticket = queryParams.getFirst("ticket");
        String destinationRaw = queryParams.getFirst("destination");
        String targetIdRaw = queryParams.getFirst("targetId"); // Rename parameter here if desired (e.g., "id")

        if (!StringUtils.hasText(ticket)) {
            log.warn("WS handshake rejected: missing ticket");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 1. Parse destination type before consuming the ticket
        WebsocketDestination destType;
        try {
            if (!StringUtils.hasText(destinationRaw)) {
                throw new IllegalArgumentException("Missing destination");
            }
            destType = WebsocketDestination.valueOf(destinationRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("WS handshake rejected: invalid destination={}", destinationRaw);
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        // 2. Parse targetId (skip if destination is USER since userId is retrieved from the ticket)
        Long targetId = null;
        if (destType != WebsocketDestination.USER) {
            try {
                if (!StringUtils.hasText(targetIdRaw)) {
                    throw new IllegalArgumentException("Missing targetId");
                }
                targetId = Long.parseLong(targetIdRaw.trim());
            } catch (NumberFormatException e) {
                log.warn("WS handshake rejected: invalid targetId={}", targetIdRaw);
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return false;
            }
        }

        // 3. Atomic GETDEL: retrieve userId and delete the ticket
        String redisKey = RedisKey.WS_TICKET.build(ticket);
        String userIdStr = redisTemplate.opsForValue().getAndDelete(redisKey);

        if (!StringUtils.hasText(userIdStr)) {
            log.warn("WS handshake rejected: invalid or expired ticket");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long userId = Long.parseLong(userIdStr);

        // 4. Authorize & build the canonical topic destination
        boolean authorized = switch (destType) {
            case TEAM -> securityService.teamAuthorized(userId, targetId);
            case CHANNEL -> securityService.channelAuthorized(userId, targetId);
            case POST -> securityService.postAuthorized(userId, targetId);
            case PRIVATE_SESSION -> securityService.privateSessionAuthorized(userId, targetId);
            case USER -> true; // Always valid since it binds directly to the ticket's own userId
        };

        if (!authorized) {
            log.warn("WS handshake forbidden: userId={} cannot access {} targetId={}", userId, destType, targetId);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        Object resolvedId = (destType == WebsocketDestination.USER) ? userId : targetId;
        String resolvedDestination = destType.getDestination(resolvedId);

        SessionContext context = SessionContext.builder()
                .userId(userId)
                .destination(resolvedDestination)
                .build();

        attributes.put(SESSION_CONTEXT_ATTR, context);
        log.debug("WS handshake accepted: {}", context);
        return true;
    }

    @Override
    public void afterHandshake(
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response,
            @NotNull WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}