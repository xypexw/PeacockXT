package com.example.peacockxt.Configuration;

import com.example.peacockxt.Models.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;

@Configuration
public class RedisConfig {

    private final ObjectMapper globalObjectMapper;

    public RedisConfig(ObjectMapper globalObjectMapper) {
        this.globalObjectMapper = globalObjectMapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,Object.class);
    }


    @Bean
    public RedisTemplate<String, Long> longRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,Long.class);
    }

    @Bean
    public RedisTemplate<String, Comment> commentRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,Comment.class);
    }

    @Bean
    public RedisTemplate<String, Team> teamRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,Team.class);
    }

    @Bean
    public RedisTemplate<String, User> userRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,User.class);
    }

    @Bean
    public RedisTemplate<String, Message> messageRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,Message.class);
    }

    @Bean
    public RedisTemplate<String, Membership> membershipRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,Membership.class);
    }

    @Bean
    public RedisTemplate<String, Channel> channelRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,Channel.class);
    }

    @Bean
    public RedisTemplate<String, CustomEmoji> customEmojiRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,CustomEmoji.class);
    }

    @Bean
    public RedisTemplate<String, DirectMessage> directMessageRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,DirectMessage.class);
    }

    @Bean
    public RedisTemplate<String, Event> eventRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,Event.class);
    }

    @Bean
    public RedisTemplate<String, Post> postRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,Post.class);
    }

    @Bean
    public RedisTemplate<String, PrivateSession> privateSessionRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf,PrivateSession.class);
    }

    private <T> RedisTemplate<String, T> buildTemplate(RedisConnectionFactory cf , Class<T> clazz) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        ObjectMapper redisMapper = globalObjectMapper.copy();
        redisMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        Hibernate6Module hibernateModule = new Hibernate6Module();
        hibernateModule.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);

        redisMapper.registerModule(hibernateModule);
        redisMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        Jackson2JsonRedisSerializer<T> jsonSerializer = new Jackson2JsonRedisSerializer<>(redisMapper, clazz);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}