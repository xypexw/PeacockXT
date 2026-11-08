package com.example.peacockxt.Infrastructure.MessageQueue;

import com.example.peacockxt.DTO.Event.InternalEvent;
import com.example.peacockxt.DTO.Event.SocketEvent;
import com.example.peacockxt.Infrastructure.Constants.WebsocketDestination;
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