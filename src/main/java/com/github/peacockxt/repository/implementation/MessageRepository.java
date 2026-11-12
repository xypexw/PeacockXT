package com.github.peacockxt.repository.implementation;

import com.github.peacockxt.models.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface MessageRepository extends JpaRepository<Message,Long> {
    @Query("SELECT m.messageId FROM Message m WHERE m.channel.channelId = :channelId AND m.messageId < :cursorId " +
            "ORDER BY m.messageId DESC ")
    List<Long> getMessageIdsByCursorId(
            @Param("channelId") Long channelId ,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.channel.channelId = :channelId AND m.messageId IN :messageIds" +
            " ORDER BY m.messageId DESC")
    List<Message> getMessagesByListOfMessageId(
            @Param("channelId") Long channelId,
            @Param("messageIds") List<Long> messageIds
    );
    Message getMessagesByMessageId(Long messageId);

}
