package com.example.peacockxt.Infrastructure.MessageQueue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicBindingInstaller {

    private final AmqpAdmin amqpAdmin;
    private final Queue nodeQueue;
    private final DirectExchange directExchange;

    private final Set<String> subscribed = ConcurrentHashMap.newKeySet();

    public void subscribeToRoutingKey(String routingKey) {
        if (subscribed.contains(routingKey)) {
            return;
        }

        synchronized (this) {
            if (!subscribed.contains(routingKey)) {
                Binding dynamicBinding = BindingBuilder
                        .bind(nodeQueue)
                        .to(directExchange)
                        .with(routingKey);

                amqpAdmin.declareBinding(dynamicBinding);
                subscribed.add(routingKey);
                log.info("Bound nodeQueue [{}] to routingKey: {}", nodeQueue.getName(), routingKey);
            }
        }
    }




}