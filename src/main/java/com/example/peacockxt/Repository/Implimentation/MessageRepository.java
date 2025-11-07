package com.example.peacockxt.Repository.Implimentation;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Service.CustomModels.MessageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface MessageRepository extends JpaRepository<Message,Float> {
    void deleteMessageByMessageId(Long messageId);
    Message getMessageByMessageId(Long messageId);

    @Query(value = "SELECT * FROM message WHERE channel_id = :channelId AND messageId < :pivotId ORDER BY messageId DESC LIMIT 20",
            nativeQuery = true)
    List<Message> getMessagesByPivotId(@Param("pivotId") Long pivotId, @Param("channelId") String channelId);

    @Query("""
        SELECT new com.example.peacockxt.Service.CustomModels.MessageResponse(
            m.messageId,m.content,m.status,m.createBy,
            m.createAt,r.messageId,r.content,r.createBy
        )
        FROM Message m LEFT JOIN m.replyTo r
        WHERE m.channelId = :channelId AND m.messageId < :pivotId
        ORDER BY m.messageId DESC
   """)
    List<MessageResponse> fetchDirectResponse(
            @Param("pivotId") Long pivotId,
            @Param("channelId") String channelId,
            Pageable pageable);


    @Query("""
        SELECT new com.example.peacockxt.Service.CustomModels.MessageResponse(
            m.messageId,m.content,m.status,m.createBy,
            m.createAt,r.messageId,r.content,r.createBy
        )
        FROM Message m LEFT JOIN m.replyTo r
        WHERE m.channelId = :channelId AND m.messageId = :pivotId
    """)
    MessageResponse fetchMessageResponseByMessageId(@Param("pivotId") Long pivotId,
                                                    @Param("channelId") String channelId);

    @Query("SELECT m.messageId from Message m where m.channelId = :channelId and m.messageId < :pivotId order by m.messageId DESC ")
    List<Long> getCurrentIndex( @Param("pivotId") Long pivotId , @Param("channelId") String channelId,Pageable pageable);



}
