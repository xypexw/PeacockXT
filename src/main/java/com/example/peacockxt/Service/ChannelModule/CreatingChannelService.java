package com.example.peacockxt.Service.ChannelModule;

import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.ChannelRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class CreatingChannelService {
    private final RedisTemplate<String, Channel> channelRedisTemplate;
    private final HashOperations<String, String, Channel> hashOps;
    private final ChannelRepository channelRepository;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket broker


    public CreatingChannelService(RedisTemplate<String, Channel> channelRedisTemplate ,
                                  ChannelRepository channelRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.channelRedisTemplate = channelRedisTemplate;
        this.channelRepository = channelRepository;
        this.hashOps = channelRedisTemplate.opsForHash();
        this.messagingTemplate = messagingTemplate;
    }
    private final String teamKeyPrefix = "TEAM::";
    private final String ChannelKeyPrefix = "CHANNEL::";
    private final int timeToLive = 10;
    private final String defaultStatus = "Default";

    @Transactional
    public void CreatingChannel(String name , String Description , String userId , Team team) {
        final String channelKey = UuidCreator.getTimeBased().toString();
        LocalDateTime now = LocalDateTime.now();
        new Channel();
        Channel channel = Channel.builder().
                channelId(channelKey).name(name).description(name).status(defaultStatus)
                .createBy(userId).updateBy(userId)
                .createAt(now).updateAt(now).team(team).build();
        channelRepository.save(channel);

        if(hashOps.putIfAbsent(teamKeyPrefix + team.getTeamId()
                , ChannelKeyPrefix + channelKey, channel)){
            channelRedisTemplate.expire(teamKeyPrefix + team.getTeamId(), timeToLive, TimeUnit.DAYS);
        }

        messagingTemplate.convertAndSend(
                "/topic/team/" + team.getTeamId() + "/channels",
                channel
        );

    }

}
