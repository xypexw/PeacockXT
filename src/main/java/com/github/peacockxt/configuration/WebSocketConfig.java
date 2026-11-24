package com.github.peacockxt.configuration;

import com.github.peacockxt.filter.websocket.UserHandshakeHandler;
import com.github.peacockxt.filter.websocket.WebSocketBindingInterceptor;
import com.github.peacockxt.filter.websocket.WebSocketGuardDecoratorFactory;
import com.github.peacockxt.filter.websocket.WsTicketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long HEARTBEAT_INTERVAL_MS = 15_000L;

    private final WebSocketBindingInterceptor webSocketBindingInterceptor;
    private final WsTicketHandshakeInterceptor wsTicketHandshakeInterceptor;
    private final UserHandshakeHandler userHandshakeHandler;
    private final WebSocketGuardDecoratorFactory webSocketGuardDecoratorFactory;

    @Bean(name = "wsHeartbeatTaskScheduler")
    public TaskScheduler wsHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(wsTicketHandshakeInterceptor)
                .setHandshakeHandler(userHandshakeHandler)
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/ws-sockjs")
                .addInterceptors(wsTicketHandshakeInterceptor)
                .setHandshakeHandler(userHandshakeHandler)
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(HEARTBEAT_INTERVAL_MS);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue")
                // [server -> client: 15s, client -> server: 15s]
                // STOMP 1.2 standard automatically uses max(client, server), so even if the client requests 1ms, the server maintains a minimum of 15s
                .setHeartbeatValue(new long[]{HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS})
                .setTaskScheduler(wsHeartbeatTaskScheduler());
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.addDecoratorFactory(webSocketGuardDecoratorFactory)
                .setMessageSizeLimit(64 * 1024)       // Maximum 64KB per message
                .setSendBufferSizeLimit(512 * 1024)   // Disconnect if send buffer exceeds 512KB (slow client)
                .setSendTimeLimit(10_000)             // Close if frame write takes longer than 10s
                .setTimeToFirstMessage(15_000);       // Kick if STOMP CONNECT is not sent within 15s after handshake
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketBindingInterceptor);
    }
}