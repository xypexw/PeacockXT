package com.github.peacockxt.service.channel;

import com.github.peacockxt.dto.event.ChannelPayload; // Nhớ tạo DTO này nhé
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.cache.TeamChannelRedisDao;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Channel;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.repository.implementation.ChannelRepository;
import com.github.peacockxt.repository.implementation.TeamRepository;
import com.github.peacockxt.service.provider.MembershipProvider;
import com.github.peacockxt.service.SnowflakeIdGenerator;
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