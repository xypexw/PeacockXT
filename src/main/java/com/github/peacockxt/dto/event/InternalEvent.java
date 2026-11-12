package com.github.peacockxt.dto.event;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InternalEvent<T>{
    private String eventType; // "CHAT_MESSAGE_SENT", "USER_TYPING"
    private T payload;
    private String destination;
}
