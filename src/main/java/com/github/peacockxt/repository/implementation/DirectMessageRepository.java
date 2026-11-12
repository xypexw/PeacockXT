package com.github.peacockxt.repository.implementation;

import com.github.peacockxt.models.DirectMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage,Long> {
    DirectMessage getDirectMessageByMessageId(Long messageId);

    @Query("SELECT dm.messageId from DirectMessage dm" +
            " where dm.messageId < :cursorId and dm.privateSession.id = :privateSessionId ORDER BY dm.messageId DESC ")
    List<Long> getMessageIdsByCursorId(
            @Param("privateSessionId") Long privateSessionId ,
            @Param("cursorId") Long cursorId,
            Pageable pageable);


    @Query("Select dm from DirectMessage dm where dm.messageId IN :messageIds ")
    List<DirectMessage> getDirectMessagesByMessageIdIn( @Param("messageIds") List<Long> messageIds);

}
