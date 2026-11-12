package com.github.peacockxt.infrastructure.messagequeue;

import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.dto.event.SocketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class DirectMessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "#{nodeQueue.name}")
    public void receiveMessage(InternalEvent<?> event) {
        SocketEvent<Object> socketEvent = SocketEvent.builder()
                .eventType(event.getEventType())
                .data(event.getPayload())
                .timestamp(Instant.now())
                .build();
        messagingTemplate.convertAndSend(event.getDestination(), socketEvent);
    }
}