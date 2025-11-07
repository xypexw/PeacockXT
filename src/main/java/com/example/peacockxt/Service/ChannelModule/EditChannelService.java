package com.example.peacockxt.Service.ChannelModule;

import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.ChannelRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class EditChannelService{
    private final ChannelRepository channelRepository;
    private final RedisTemplate<String,Channel> redisTemplate;
    private final String channelKeyPrefix = "CHANNEL::";
    private final String teamKeyPrefix = "TEAM::";
    private final String editStatus = "Modified";
    public EditChannelService(ChannelRepository channelRepository,RedisTemplate<String,Channel> redisTemplate) {
        this.channelRepository = channelRepository;
        this.redisTemplate = redisTemplate;
    }



    @Transactional
    public void creatingChannel( String channelId , String name , String Description , String userId , Team team) {
        Channel channel = channelRepository.getChannelByChannelId(channelId);
        LocalDateTime now = LocalDateTime.now();
        channel.setName(name);
        channel.setStatus(editStatus);
        channel.setDescription(Description);
        channel.setUpdateBy(userId);
        channel.setUpdateAt(now);
        channel.setTeam(team);
        channelRepository.save(channel);
        deleteChannelCache(team.getTeamId(), channelId);
    }

    private void deleteChannelCache( String teamId , String channelId){
        redisTemplate.opsForHash().delete(teamKeyPrefix + teamId ,channelKeyPrefix + channelId);
    }

}
