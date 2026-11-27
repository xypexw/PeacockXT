package com.github.peacockxt.service.channel;

import com.github.peacockxt.dto.response.ChannelResponse;
import com.github.peacockxt.infrastructure.cache.TeamChannelRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheChannel;
import com.github.peacockxt.infrastructure.cache.models.CacheMembership;
import com.github.peacockxt.infrastructure.cache.models.CacheTeam;
import com.github.peacockxt.models.Channel;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.ChannelRepository;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.provider.TeamProvider;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class ReadChannelService {
    private final ChannelRepository channelRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;
    private final int cacheLimit = 500;
    private final int limit = 40;
    private final String DELETE_STATUS = "DELETED";

    private final TeamProvider teamProvider;
    private final MembershipProvider membershipProvider;

    public List<ChannelResponse> readChannels(Long teamId, Long membershipId, Long userId, Long cursorId) {
        CacheTeam team = teamProvider.getTeam(teamId);
        if (team == null) {
            throw new BusinessLogicException("No team found");
        }
        CacheMembership membership = membershipProvider.getMembership(membershipId);
        if (membership == null) {
            throw new BusinessLogicException("No membership found");
        }
        boolean isCorrectTeam = membership.getTeamId().equals(team.getTeamId());
        boolean isOwner = membership.getUserId().equals(userId);

        if (!isCorrectTeam || !isOwner) {
            throw new BusinessLogicException("You are not allowed to read this channel");
        }

        List<Long> channelIds = getChannelIds(teamId, cursorId);
        if (channelIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<CacheChannel> channelList = getChannels(channelIds);
        return channelResponsesListBuilder(channelList);
    }

    List<Long> getChannelIds(Long teamId, Long cursorId) {
        Set<Long> cachedChannelIds = teamChannelRedisDao.getChannelIdsBeforeCursor(teamId, cursorId, limit);
        if (cachedChannelIds != null && !cachedChannelIds.isEmpty()) {
            return new ArrayList<>(cachedChannelIds);
        }

        if (cursorId == null) {
            Pageable pageable = PageRequest.of(0, cacheLimit);
            List<Long> idList = channelRepository.getIndexByChannelIdAndCursorId(teamId, Long.MAX_VALUE, pageable);
            if (idList == null || idList.isEmpty()) {
                return Collections.emptyList();
            }
            teamChannelRedisDao.saveChannelIndexesBatch(teamId, idList);
            return idList.subList(0, Math.min(idList.size(), limit));
        } else {
            Pageable pageable = PageRequest.of(0, limit);
            List<Long> idList = channelRepository.getIndexByChannelIdAndCursorId(teamId, cursorId, pageable);
            return (idList == null) ? Collections.emptyList() : idList;
        }
    }

    List<CacheChannel> getChannels(List<Long> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CacheChannel> cached = teamChannelRedisDao.multiGetChannelInfo(channelIds);
        Map<Long, CacheChannel> channelPool = new HashMap<>(channelIds.size());
        if (cached != null) {
            for (CacheChannel c : cached) {
                if (c != null) {
                    channelPool.put(c.getChannelId(), c);
                }
            }
        }

        List<Long> missingIds = new ArrayList<>();
        for (Long id : channelIds) {
            if (!channelPool.containsKey(id)) {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            List<Channel> channelInDb = channelRepository.getChannelsByChannelIdIn(missingIds);
            List<CacheChannel> toCache = new ArrayList<>(channelInDb.size());
            for (Channel entity : channelInDb) {
                CacheChannel cacheChannel = toCacheChannel(entity);
                channelPool.put(cacheChannel.getChannelId(), cacheChannel);
                toCache.add(cacheChannel);
            }
            if (!toCache.isEmpty()) {
                teamChannelRedisDao.multiSetChannelInfo(toCache);
            }
        }

        List<CacheChannel> result = new ArrayList<>(channelIds.size());
        for (Long id : channelIds) {
            CacheChannel c = channelPool.get(id);
            if (c != null) {
                result.add(c);
            }
        }
        return result;
    }

    List<ChannelResponse> channelResponsesListBuilder(List<CacheChannel> channels) {
        List<ChannelResponse> result = new ArrayList<>();
        for (CacheChannel channel : channels) {
            if (channel != null && !DELETE_STATUS.equals(channel.getStatus())) {
                ChannelResponse channelResponse = new ChannelResponse();
                channelResponse.setChannelId(channel.getChannelId().toString());
                channelResponse.setName(channel.getName());
                channelResponse.setCreateBy(channel.getCreateBy());
                channelResponse.setDescription(channel.getDescription());
                channelResponse.setStatus(channel.getStatus());
                result.add(channelResponse);
            }
        }
        return result;
    }

    private CacheChannel toCacheChannel(Channel entity) {
        return CacheChannel.builder()
                .channelId(entity.getChannelId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createBy(entity.getCreateBy())
                .updateBy(entity.getUpdateBy())
                .createAt(entity.getCreateAt())
                .updateAt(entity.getUpdateAt())
                .teamId(entity.getTeamId())
                .build();
    }
}
