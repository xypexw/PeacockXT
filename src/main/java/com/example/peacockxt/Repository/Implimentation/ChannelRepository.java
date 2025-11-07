package com.example.peacockxt.Repository.Implimentation;

import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    void deleteChannelByChannelId(String channelId);
    @Query("select m from Message m where m.channel.channelId = :channelId order by m.messageId desc limit 20 ")
    List<Message> getMessagesByChannelId(@Param("channelId") String channelId);
    Channel getChannelByChannelId(String channelId);
    @Query("select c from Channel c where c.team.teamId = :teamId")
    List<Channel> getChannelsByTeam(@Param("teamId")  String teamId);

}
