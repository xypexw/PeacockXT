package com.example.peacockxt.Service.ChannelModule;

import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Repository.Implimentation.ChannelRepository;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

public class DeleteChannelService {
    private final ChannelRepository channelRepository;
    private final RedisTemplate<String,Channel> channelRedisTemplate;
    private final HashOperations<String, String, Channel> hashOperations;
    private final String channelKeyPrefix = "CHANNEL::";
    private final String teamKeyPrefix = "TEAM::";
    public DeleteChannelService(ChannelRepository channelRepository, RedisTemplate<String,Channel> channelRedisTemplate) {
        this.channelRepository = channelRepository;
        this.channelRedisTemplate = channelRedisTemplate;
        this.hashOperations = channelRedisTemplate.opsForHash();
    }

    public void DeleteChannelCache( String teamId  , String channelId) {
        channelRepository.deleteChannelByChannelId(channelId);
        hashOperations.delete(teamKeyPrefix+teamId,channelKeyPrefix + channelId);
    }

}
