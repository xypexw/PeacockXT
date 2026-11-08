package com.example.peacockxt.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "direct_message",
        indexes = {
                @Index(name = "idx_session_message", columnList = "session_id, message_id")
        }
)
public class DirectMessage {

    @Id
    @Column(name = "message_id")
    private Long messageId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    private String status;

    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @Column(name = "send_at")
    private LocalDateTime sendAt;
    /* SELECT m.messageId , m.content , m.senderId , m.status , m.createAt , m.replyToId , r.senderId
      FROM DirectMessage m
    LEFT JOIN DirectMessage r ON m.replyToId = r.messageId
    WHERE m.privateSession.id = :sessionId ORDER BY m.messageId DESC */
    @Column(name = "reply_to_id")
    private Long replyToId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PrivateSession privateSession;
}