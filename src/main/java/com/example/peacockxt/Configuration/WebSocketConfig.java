package com.example.peacockxt.Configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Client sẽ connect vào đây
        registry.addEndpoint("/ws").setAllowedOrigins("*");
        // Nếu muốn hỗ trợ fallback cho trình duyệt cũ thì thêm .withSockJS()
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /app là prefix của các MessageMapping từ client gửi lên server
        registry.setApplicationDestinationPrefixes("/app");

        // /topic là prefix cho server broadcast tới client
        registry.enableSimpleBroker("/topic");
    }
}