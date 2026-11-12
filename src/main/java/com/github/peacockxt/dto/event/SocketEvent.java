package com.github.peacockxt.dto.event;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SocketEvent<T> {
    private String eventType; // "CHAT_MESSAGE_SENT", "USER_TYPING"
    private Instant timestamp;
    private T data;           // Payload cụ thể của event
}
