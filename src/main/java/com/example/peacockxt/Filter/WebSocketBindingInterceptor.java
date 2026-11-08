package com.example.peacockxt.Filter;

import com.example.peacockxt.Infrastructure.MessageQueue.DynamicBindingInstaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketBindingInterceptor implements ChannelInterceptor {

    private final DynamicBindingInstaller dynamicBindingInstaller;

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Intercept when the client sends a SUBSCRIBE command via WebSocket
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination(); // e.g., "/topic/channel.123" or "/topic/team.456"

            if (destination != null && destination.startsWith("/topic/")) {
                log.debug("Client [session={}] subscribing to destination: {}",
                        accessor.getSessionId(), destination);
                // Bind the current server's nodeQueue to RabbitMQ using destination as the routing key
                dynamicBindingInstaller.subscribeToRoutingKey(destination);
            }
        }

        return message;
    }
}