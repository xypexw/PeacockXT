package com.example.peacockxt.Configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQDirectConfig {

    public static final String DIRECT_EXCHANGE_NAME = "the.direct.exchange";

    // 1. Initialize Direct Exchange
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE_NAME);
    }

    // 2. Specific Queue for this Node (automatically deleted when the node disconnects)
    @Bean
    public Queue nodeQueue() {
        return new AnonymousQueue();
    }

    // 3. This Bean to allow RabbitMQ to automatically parse JSON <-> Object
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

}
