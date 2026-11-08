package com.example.peacockxt.Service.Channel;

import com.example.peacockxt.DTO.Respone.ChannelResponse;
import com.example.peacockxt.Infrastructure.Cache.TeamChannelRedisDao;
import com.example.peacockxt.Models.Channel;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.Membership;
import com.example.peacockxt.Models.Team;
import com.example.peacockxt.Repository.Imlpementation.ChannelRepository;
import com.example.peacockxt.Service.Provider.MembershipProvider;
import com.example.peacockxt.Service.Provider.TeamProvider;
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


    public List<ChannelResponse> readChannels(Long teamId , Long membershipId , Long userId , Long cursorId ) {

        Team team = teamProvider.getTeam(teamId);
        if (team == null) {
            throw new BusinessLogicException("No team found");
        }
        Membership membership = membershipProvider.getMembership(membershipId);
        if (membership == null) {
            throw new BusinessLogicException("No membership found");
        }
        boolean isCorrectTeam = membership.getTeamId().equals(team.getTeamId());
        boolean isOwner = membership.getUserId().equals(userId);

        if (!isCorrectTeam || !isOwner) {
            throw new BusinessLogicException("You are not allowed to read this channel");
        }

        List<Long> channelIds = getChannelIds(teamId, cursorId);
        List<Channel> channelList = getChannels(channelIds);
        return channelResponsesListBuilder(channelList);
    }

    List<Long> getChannelIds(Long teamId ,Long cursorId ) {
        Set<Long> cachedChannelIds = teamChannelRedisDao.getChannelIdsFromTeam(teamId);

        List<Long> channelIds = new ArrayList<>(cachedChannelIds);
        if(channelIds.isEmpty()) {
            Pageable pageable = PageRequest.of(0, cacheLimit);
            if(cursorId==null){
                cursorId = Long.MAX_VALUE;
            }
            List<Long> idList = channelRepository.getIndexByChannelIdAndCursorId(teamId,cursorId,pageable);
            teamChannelRedisDao.saveChannelIndexesBatch(teamId,idList);
            return idList.subList(0,Math.min(idList.size(),limit));
        }
        else return channelIds;
    }

    List<Channel> getChannels(List<Long> channelIds) {
        List<Channel> channels = teamChannelRedisDao.multiGetChannelInfo(channelIds);
        List<Long> missingIds = new ArrayList<>();
        for(int i = 0 ; i < channels.size() ; i++) {
            if(channels.get(i)==null) {
                missingIds.add(channelIds.get(i));
            }
        }
        List<Channel> channelInDb = channelRepository.getChannelsByChannelIdIn(missingIds);
        Map<Long,Channel> channelPool = new HashMap<>();

        for(Channel c : channelInDb) {
            channelPool.put(c.getChannelId(),c);
        }

        teamChannelRedisDao.multiSetChannelInfo(channelInDb);

        List<Channel> result = new ArrayList<>();
        for( int i = 0 ; i < channels.size() ; i++ ) {
            Channel c = channels.get(i);
            if(c==null){
                Long key = channelIds.get(i);
                c = channelPool.get(key);
            }
            if(c!=null) {
                result.add(c);
            }
        }
        return result;

    }

    List<ChannelResponse> channelResponsesListBuilder(List<Channel> channels) {
        List<ChannelResponse> result = new ArrayList<>();
        for(Channel channel : channels) {
            ChannelResponse channelResponse = new ChannelResponse();
            if(channel!=null && !channel.getStatus().equals(DELETE_STATUS)) {
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

}
