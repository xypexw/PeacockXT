package com.github.peacockxt.filter.websocket;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionContext {
    private Long userId;
    private String destination; // Example: "/topic/team.123" or "/topic/user.45"
}
