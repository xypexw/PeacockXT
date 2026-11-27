package com.github.peacockxt.service.provider;

import com.github.peacockxt.infrastructure.cache.TeamChannelRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheChannel;
import com.github.peacockxt.models.Channel;
import com.github.peacockxt.models.customexception.DatabaseException;
import com.github.peacockxt.repository.implementation.ChannelRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ChannelProvider {

    private final ChannelRepository channelRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;

    public CacheChannel getChannel(Long channelId) {
        if (channelId == null) return null;
        try {
            CacheChannel cachedChannel = teamChannelRedisDao.getChannelInfo(channelId);
            if (cachedChannel != null) {
                return cachedChannel;
            }
            Channel channel = channelRepository.getChannelByChannelId(channelId);
            if (channel == null) {
                return null;
            }
            CacheChannel cacheChannel = toCacheChannel(channel);
            saveChannelInCache(cacheChannel);
            return cacheChannel;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query Channel from database for channelId: " + channelId, e);
        }
    }

    public boolean hasChannel(Long channelId) {
        if (channelId == null) return false;
        if (hasChannelInCache(channelId)) {
            return true;
        }
        try {
            Channel channel = channelRepository.getChannelByChannelId(channelId);
            if (channel == null) {
                return false;
            }
            saveChannelInCache(toCacheChannel(channel));
            return true;
        } catch (Exception e) {
            throw new DatabaseException("Failed to query Channel from database for channelId: " + channelId, e);
        }
    }

    private boolean hasChannelInCache(Long channelId) {
        return teamChannelRedisDao.hasChannelInfo(channelId);
    }

    private void saveChannelInCache(CacheChannel channel) {
        teamChannelRedisDao.saveChannelInfo(channel.getChannelId(), channel);
    }

    private CacheChannel toCacheChannel(Channel channel) {
        return CacheChannel.builder()
                .channelId(channel.getChannelId())
                .name(channel.getName())
                .description(channel.getDescription())
                .status(channel.getStatus())
                .createBy(channel.getCreateBy())
                .updateBy(channel.getUpdateBy())
                .createAt(channel.getCreateAt())
                .updateAt(channel.getUpdateAt())
                .teamId(channel.getTeamId())
                .build();
    }
}
