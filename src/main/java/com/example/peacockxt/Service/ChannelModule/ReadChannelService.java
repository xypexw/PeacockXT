package com.example.peacockxt.Service.ChannelModule;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Repository.Implimentation.ChannelRepository;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ReadChannelService {

    private final ChannelRepository channelRepository;
    private final RedisTemplate<String, Channel> channelRedisTemplate;
    private final HashOperations<String, String, Channel> hashOperations;
    private final String teamKeyPrefix = "TEAM::";
    private final String channelKeyPrefix = "CHANNEL::";
    private final long timeToLive = 7;

    public ReadChannelService(ChannelRepository channelRepository, RedisTemplate<String, Channel> channelRedisTemplate){
        this.channelRepository = channelRepository;
        this.channelRedisTemplate = channelRedisTemplate;
        this.hashOperations = channelRedisTemplate.opsForHash();
    }

    public Channel readSingleChannel(String channelId){

    }

}
