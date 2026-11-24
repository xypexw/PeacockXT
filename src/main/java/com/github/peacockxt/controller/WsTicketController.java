package com.github.peacockxt.controller;

import com.github.peacockxt.infrastructure.constants.RedisKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ws-ticket")
@RequiredArgsConstructor
@Slf4j
public class WsTicketController {

    private final StringRedisTemplate redisTemplate;

    public record WsTicketResponse(String ticket, long expiresInSeconds) {}

    @PostMapping
    public ResponseEntity<WsTicketResponse> issueTicket(
            @RequestHeader("X-User-Id") Long userId
    ) {
        String ticket = UUID.randomUUID().toString();
        String redisKey = RedisKey.WS_TICKET.build(ticket);

        redisTemplate.opsForValue().set(
                redisKey,
                String.valueOf(userId),
                RedisKey.WS_TICKET.getTtl()
        );

        log.debug("Issued WS ticket for userId={}, key={}", userId, redisKey);

        return ResponseEntity.ok(
                new WsTicketResponse(ticket, RedisKey.WS_TICKET.getTtl().toSeconds())
        );
    }
}