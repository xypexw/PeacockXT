package com.example.peacockxt.Models.SystemModule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "direct_message",
        indexes = {
                @Index(name = "idx_sessionId_messageIndex" , columnList = "session_id,message_index")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectMessage {
    @Id
    private Long messageId;
    @Column(name = "message_index")
    private int messageIndex;
    private String content;
    private String senderId;
    private String status;
    private LocalDateTime updatedAt;
    private LocalDateTime createAt;
    @Column(name = "send_at")
    private LocalDateTime sendAt;

    @ManyToOne
    DirectMessage replyId;

    @ManyToOne
    @JoinColumn(name = "session_id" , referencedColumnName = "sessionId")
    PrivateSession privateSession;

}
