package com.github.peacockxt.service_temp.channel_temp;

import com.github.peacockxt.dto_temp.event_temp.ChannelPayload; // Nhớ tạo DTO này nhé
import com.github.peacockxt.dto_temp.event_temp.InternalEvent;
import com.github.peacockxt.infrastructure_temp.cache_temp.TeamChannelRedisDao;
import com.github.peacockxt.infrastructure_temp.constants_temp.WebsocketDestination;
import com.github.peacockxt.infrastructure_temp.messagequeue_temp.LocalEventDispatcher;
import com.github.peacockxt.models_temp.Channel;
import com.github.peacockxt.models_temp.customexception_temp.BusinessLogicException;
import com.github.peacockxt.models_temp.Membership;
import com.github.peacockxt.models_temp.Team;
import com.github.peacockxt.repository_temp.implementation.ChannelRepository;
import com.github.peacockxt.repository_temp.implementation.TeamRepository;
import com.github.peacockxt.service_temp.provider_temp.MembershipProvider;
import com.github.peacockxt.service_temp.SnowflakeIdGenerator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CreateChannelService {

    private static final String DEFAULT_STATUS = "DEFAULT";
    private final String event_name = "CREATE_CHANNEL";

    private final TeamChannelRedisDao teamChannelRedisDao;
    private final ChannelRepository channelRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final TeamRepository teamRepository;
    private final MembershipProvider membershipProvider;
    private final LocalEventDispatcher localEventDispatcher;

    @Transactional
    public void createChannel(String name, String description, Long userId, Long teamId , Long membershipId) {

        Membership membership = membershipProvider.getMembership(membershipId);
        if(membership == null) {
            throw new BusinessLogicException("Membership not found");
        }
        if(!membership.getUserId().equals(userId) || !membership.getTeamId().equals(teamId)) {
            throw  new BusinessLogicException("UserId or Team Id in Membership mismatch");
        }

        Channel channel = channelBuilder(name, description, userId, teamId);
        channelRepository.save(channel);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(channel, teamId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                teamChannelRedisDao.addChannelToTeam(teamId, channel.getChannelId(), channel.getChannelId());
                teamChannelRedisDao.saveChannelInfo(teamId, channel);
            }
        });
    }

    private Channel channelBuilder(String name, String description, Long userId, Long teamId) {
        Team team = teamRepository.getReferenceById(teamId);
        return Channel.builder()
                .channelId(snowflakeIdGenerator.nextId())
                .name(name)
                .description(description)
                .createBy(userId)
                .createAt(LocalDateTime.now())
                .updateAt(LocalDateTime.now())
                .updateBy(userId)
                .status(DEFAULT_STATUS)
                .team(team)
                .build();
    }


    private InternalEvent<ChannelPayload> internalEventBuilder(Channel channel, Long teamId) {
        ChannelPayload payload = ChannelPayload.builder()
                .channelId(channel.getChannelId())
                .name(channel.getName())
                .description(channel.getDescription())
                .status(channel.getStatus())
                .createAt(channel.getCreateAt())
                .teamId(teamId)
                .build();

        return InternalEvent.<ChannelPayload>builder()
                .payload(payload)
                .eventType(event_name)
                .destination(WebsocketDestination.TEAM.getDestination(teamId))
                .build();
    }
}