package com.example.peacockxt.Policy;

import com.example.peacockxt.Models.Channel;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.Membership;
import com.example.peacockxt.Models.Message;
import com.example.peacockxt.Service.Provider.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@AllArgsConstructor
public class MessagePolicy {
    private final MembershipProvider membershipProvider;
    private final TeamPermission teamPermission;
    private final ChannelProvider channelProvider;
    private final UserProvider userProvider;

    public void validateCanDeleteMessage(Long channelId, Message message) {
        validateMessageAndChannel(channelId, message);

        if (!userProvider.hasUser(message.getSenderId())) {
            throw new BusinessLogicException("User is not authorized to delete this message");
        }
    }

    public void validateCanEditMessage(Long channelId, Message message) {
        validateMessageAndChannel(channelId, message);

        if (!userProvider.hasUser(message.getSenderId())) {
            throw new BusinessLogicException("User is not authorized to edit this message");
        }
    }

    private void validateMessageAndChannel(Long channelId, Message message) {
        Channel channel = channelProvider.getChannel(channelId);
        if(channel == null) {
            throw new BusinessLogicException("Channel with id " + channelId + " does not exist");
        }
        if(channel.getStatus().equals("DELETED"))
            throw new BusinessLogicException("Channel with id " + channelId + " is DELETED");
        if (message == null) {
            throw new BusinessLogicException("Message does not exist");
        }
    }


    public void validateCanCreateMessage(Long channelId, Long membershipId , Long userId ) {
        Channel channel = channelProvider.getChannel(channelId);
        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User is not exist");
        }
        Membership membership = membershipProvider.getMembership(membershipId);
        if(membership == null) {
            throw new BusinessLogicException("Membership does not exist");
        }
        if(teamPermission.isReader(membershipId, userId)) {
            throw new BusinessLogicException("creating message is not allowed for reader membership");
        }
        if(channel.getStatus().equals("DELETED")){
            throw new BusinessLogicException("creating message is not allowed for deleted channel");
        }
    }

    public void validateCanReadMessage(Long channelId, Long membershipId ) {
        Channel channel = channelProvider.getChannel(channelId);
        if(channel == null) {
            throw new BusinessLogicException("Channel with id " + channelId + " does not exist");
        }
        if(channel.getStatus().equals("DELETED")){
            throw new BusinessLogicException("read message is not allowed for deleted channel");
        }
        Membership membership = membershipProvider.getMembership(membershipId);
        if(membership == null) {
            throw new BusinessLogicException("Membership does not exist");
        }
        if(!channel.getTeam().getTeamId().equals(membership.getTeamId())) {
            throw new BusinessLogicException("membership is not belong to this team");
        }
    }

}