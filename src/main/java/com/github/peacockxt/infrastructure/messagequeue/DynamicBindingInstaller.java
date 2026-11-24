package com.github.peacockxt.infrastructure.messagequeue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicBindingInstaller {

    private final AmqpAdmin amqpAdmin;
    private final Queue nodeQueue;
    private final DirectExchange directExchange;

    private final ConcurrentHashMap<String, Integer> subscriptionCounts = new ConcurrentHashMap<>();

    public void subscribeToRoutingKey(String routingKey) {
        subscriptionCounts.compute(routingKey, (key, currentCount) -> {
            if (currentCount == null || currentCount == 0) {
                Binding dynamicBinding = createBinding(key);
                amqpAdmin.declareBinding(dynamicBinding);
                log.info("Bound nodeQueue [{}] to routingKey: {} (count: 1)", nodeQueue.getName(), key);
                return 1;
            }
            int nextCount = currentCount + 1;
            log.debug("Incremented count for routingKey [{}]: {}", key, nextCount);
            return nextCount;
        });
    }

    public void unsubscribeFromRoutingKey(String routingKey) {
        subscriptionCounts.computeIfPresent(routingKey, (key, currentCount) -> {
            int nextCount = currentCount - 1;
            if (nextCount <= 0) {
                try {
                    Binding dynamicBinding = createBinding(key);
                    amqpAdmin.removeBinding(dynamicBinding);
                    log.info("Unbound nodeQueue [{}] from routingKey: {} (count: 0)", nodeQueue.getName(), key);
                } catch (Exception e) {
                    log.error("Failed to remove binding for routingKey [{}]", key, e);
                }
                return null;
            }
            log.debug("Decremented count for routingKey [{}]: {}", key, nextCount);
            return nextCount;
        });
    }

    public int getSubscriberCount(String routingKey) {
        return subscriptionCounts.getOrDefault(routingKey, 0);
    }

    private Binding createBinding(String routingKey) {
        return BindingBuilder
                .bind(nodeQueue)
                .to(directExchange)
                .with(routingKey);
    }
}