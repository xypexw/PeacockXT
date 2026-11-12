package com.github.peacockxt.service.channel;

import com.github.peacockxt.dto.event.ChannelPayload;
import com.github.peacockxt.dto.event.InternalEvent;
import com.github.peacockxt.infrastructure.cache.TeamChannelRedisDao;
import com.github.peacockxt.infrastructure.constants.WebsocketDestination;
import com.github.peacockxt.infrastructure.messagequeue.LocalEventDispatcher;
import com.github.peacockxt.models.Channel;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.models.Membership;
import com.github.peacockxt.repository.implementation.ChannelRepository;
import com.github.peacockxt.service.provider.ChannelProvider;
import com.github.peacockxt.service.provider.MembershipProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;


@Service
@AllArgsConstructor
public class UpdateChannelService {

    private final ChannelRepository channelRepository;
    private final TeamChannelRedisDao teamChannelRedisDao;
    private final MembershipProvider membershipProvider;
    private final ChannelProvider channelProvider;

    private final LocalEventDispatcher localEventDispatcher;

    private final String editedStatus = "EDITED";
    private final String event_name = "UPDATE_CHANNEL";

    @Transactional
    public void updateChannel(Long channelId, String name, String description,
                              Long userId, Long teamId, Long membershipId) {

        Membership membership = membershipProvider.getMembership(membershipId);
        Channel channel = channelProvider.getChannel(channelId);

        if(membership == null) {
            throw new BusinessLogicException("Membership not found");
        }
        if(!membership.getUserId().equals(userId) || !membership.getTeamId().equals(teamId)) {
            throw new BusinessLogicException("UserId or Team Id in Membership mismatch");
        }

        if(channel == null) {
            throw new BusinessLogicException("Channel not found");
        }

        if(membership.getRole().equals("READER")){
            throw new BusinessLogicException("Doesn't have permission to update channel");
        }

        boolean isCreator = channel.getCreateBy().equals(membership.getUserId());
        boolean isOwner = membership.getRole().equals("OWNER");
        boolean isAdmin = membership.getRole().equals("ADMIN");
        if (!isCreator && !isOwner && !isAdmin) {
            throw new BusinessLogicException("Doesn't have permission to update channel");
        }

        channel.setName(name);
        channel.setDescription(description);
        channel.setUpdateBy(membership.getUserId());
        channel.setUpdateAt(LocalDateTime.now());
        channel.setStatus(editedStatus);

        channelRepository.save(channel);

        localEventDispatcher.dispatchToRabbitMQ(internalEventBuilder(channel, teamId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                teamChannelRedisDao.deleteChannelInfo(channelId);
            }
        });
    }

    private InternalEvent<ChannelPayload> internalEventBuilder(Channel channel, Long teamId) {
        ChannelPayload payload = ChannelPayload.builder()
                .channelId(channel.getChannelId())
                .name(channel.getName())
                .description(channel.getDescription())
                .status(channel.getStatus())
                .teamId(teamId)
                .build();

        return InternalEvent.<ChannelPayload>builder()
                .eventType(event_name)
                .payload(payload)
                .destination(WebsocketDestination.TEAM.getDestination(teamId))
                .build();
    }
}