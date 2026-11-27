package com.github.peacockxt.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.peacockxt.infrastructure.cache.models.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private final ObjectMapper globalObjectMapper;

    public RedisConfig(ObjectMapper globalObjectMapper) {
        this.globalObjectMapper = globalObjectMapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, Object.class);
    }

    @Bean
    public RedisTemplate<String, Long> longRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, Long.class);
    }

    @Bean
    public RedisTemplate<String, CacheComment> commentRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CacheComment.class);
    }

    @Bean
    public RedisTemplate<String, CacheTeam> teamRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CacheTeam.class);
    }

    @Bean
    public RedisTemplate<String, CacheUser> userRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CacheUser.class);
    }

    @Bean
    public RedisTemplate<String, CacheMessage> messageRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CacheMessage.class);
    }

    @Bean
    public RedisTemplate<String, CacheMembership> membershipRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CacheMembership.class);
    }

    @Bean
    public RedisTemplate<String, CacheChannel> channelRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CacheChannel.class);
    }

    @Bean
    public RedisTemplate<String, CacheCustomEmoji> customEmojiRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CacheCustomEmoji.class);
    }

    @Bean
    public RedisTemplate<String, CacheDirectMessage> directMessageRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CacheDirectMessage.class);
    }

    @Bean
    public RedisTemplate<String, CacheEvent> eventRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CacheEvent.class);
    }

    @Bean
    public RedisTemplate<String, CachePost> postRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CachePost.class);
    }

    @Bean
    public RedisTemplate<String, CachePrivateSession> privateSessionRedisTemplate(RedisConnectionFactory cf) {
        return buildTemplate(cf, CachePrivateSession.class);
    }

    private <T> RedisTemplate<String, T> buildTemplate(RedisConnectionFactory cf, Class<T> clazz) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        ObjectMapper redisMapper = globalObjectMapper.copy();
        redisMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        redisMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        Jackson2JsonRedisSerializer<T> jsonSerializer = new Jackson2JsonRedisSerializer<>(redisMapper, clazz);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
