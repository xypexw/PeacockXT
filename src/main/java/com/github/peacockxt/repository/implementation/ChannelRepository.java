package com.github.peacockxt.repository.implementation;

import com.github.peacockxt.models.Channel;
import com.github.peacockxt.models.Team;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChannelRepository extends JpaRepository<Channel,Long> {
    Channel getChannelByChannelId(Long channelId);

    @Query("SELECT c.channelId FROM Channel c " +
            "WHERE c.team.teamId = :teamId AND c.channelId < :cursorId  AND c.status != 'DELETED' " +
            "ORDER BY c.channelId DESC")
    List<Long> getIndexByChannelIdAndCursorId(@Param("teamId") Long teamId,
                                           @Param("cursorId") Long cursorId,
                                           Pageable pageable);

    @Query("select c from Channel c where c.channelId IN :channelIds")
    List<Channel> getChannelsByChannelIdIn(@Param("channelIds")List<Long> channelIds);

    Long team(Team team);
}
