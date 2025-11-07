package com.example.peacockxt.Configuration;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Models.GroupModule.PostModule.Post;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.UserModule.User;
import com.example.peacockxt.Service.CustomModels.MessageResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
@Configuration
public class RedisConfig {

    private <K,V> RedisTemplate<K,V> builderTemplate(
            RedisConnectionFactory redisConnectionFactory,
            RedisSerializer<K>  keySerializer,
            RedisSerializer<V> valueSerializer) {
        RedisTemplate<K,V> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setValueSerializer(valueSerializer);
        return  redisTemplate;
    };

    @Bean
    public RedisTemplate<String, User> userRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        Jackson2JsonRedisSerializer<User>  jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(User.class);
        return builderTemplate(redisConnectionFactory, new StringRedisSerializer(), jackson2JsonRedisSerializer);
    }

    @Bean
    public RedisTemplate<String,Post> postRedisTemplate(RedisConnectionFactory redisConnectionFactory){
        Jackson2JsonRedisSerializer<Post>  jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Post.class);
        return builderTemplate(redisConnectionFactory, new StringRedisSerializer(), jackson2JsonRedisSerializer);
    }

    @Bean
    public RedisTemplate<String, Team> groupRedisTemplate(RedisConnectionFactory redisConnectionFactory){
        Jackson2JsonRedisSerializer<Team> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Team.class);
        return builderTemplate(redisConnectionFactory, new StringRedisSerializer(), jackson2JsonRedisSerializer);

    }

    public HashOperations<String, String, Channel> hashOperations(RedisTemplate<String, Channel> redisTemplate) {
        return redisTemplate.opsForHash();
    }

    @Bean
    public RedisTemplate<String, Channel> channelRedisTemplate(RedisConnectionFactory redisConnectionFactory){
        Jackson2JsonRedisSerializer<Channel> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Channel.class);
        return builderTemplate(redisConnectionFactory,new StringRedisSerializer(),jackson2JsonRedisSerializer);
    }

    @Bean
    public RedisTemplate<String, MessageResponse> messageRedisTemplate(RedisConnectionFactory redisConnectionFactory){
        Jackson2JsonRedisSerializer<MessageResponse> jackson2JsonRedisSerializer
                = new Jackson2JsonRedisSerializer<>(MessageResponse.class);
        return builderTemplate(redisConnectionFactory,new StringRedisSerializer(),jackson2JsonRedisSerializer);
    }

    @Bean
    public RedisTemplate<String, String > stringXRedisTemplate(RedisConnectionFactory redisConnectionFactory){
        return builderTemplate(redisConnectionFactory,new StringRedisSerializer(),new StringRedisSerializer());
    }

}
