package com.example.peacockxt.Service.Provider;

import com.example.peacockxt.Models.Channel;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Repository.Imlpementation.ChannelRepository;
import com.example.peacockxt.Infrastructure.Cache.TeamChannelRedisDao;
import com.example.peacockxt.Service.RedisRetryManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ChannelProvider {

    private final ChannelRepository channelRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;
    private final RedisRetryManager redisRetryManager;

    public Channel getChannel(Long channelId) {
        try {
            // 1. Get from Cache
            Channel cachedChannel = teamChannelRedisDao.getChannelInfo(channelId);
            if (cachedChannel != null) {
                return cachedChannel;
            }
            // 2. Cache Miss -> Query DB
            Channel channel = channelRepository.getChannelByChannelId(channelId);
            // If not found in DB either -> Return null
            if (channel == null) {
                return null;
            }
            // 3. Found in DB -> Save to cache and return
            saveChannelInCache(channel);
            return channel;

        } catch (Exception e) {
            throw new DatabaseException("Failed to query Channel from database for channelId: " + channelId, e);
        }
    }

    public boolean hasChannel(Long channelId) {
        if (hasChannelInCache(channelId)) {
            return true;
        }
        try {
            Channel channel = channelRepository.getChannelByChannelId(channelId);
            if (channel == null) {
                return false;
            }
            // Found in DB -> Backfill Cache and return true
            saveChannelInCache(channel);
            return true;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query Channel from database for channelId: " + channelId, e);
        }
    }

    private boolean hasChannelInCache(Long channelId) {
        return teamChannelRedisDao.hasChannelInfo(channelId);
    }

    private void saveChannelInCache(Channel channel) {
        teamChannelRedisDao.saveChannelInfo(channel.getChannelId(), channel);
    }
}