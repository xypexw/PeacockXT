package com.example.peacockxt.Infrastructure.MessageQueue;

import com.example.peacockxt.Configuration.RabbitMQDirectConfig;
import com.example.peacockxt.DTO.Event.InternalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocalEventDispatcher {

    private final RabbitTemplate rabbitTemplate;

    public void dispatchToRabbitMQ(InternalEvent<?> event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendToRabbit(event);
                }
            });
        } else {
            sendToRabbit(event);
        }
    }

    private void sendToRabbit(InternalEvent<?> event) {
        log.info("Captured internal event [{}], publishing to RabbitMQ with routingKey: {}",
                event.getEventType(), event.getDestination());
        rabbitTemplate.convertAndSend(
                RabbitMQDirectConfig.DIRECT_EXCHANGE_NAME,
                event.getDestination(),
                event
        );
    }
}