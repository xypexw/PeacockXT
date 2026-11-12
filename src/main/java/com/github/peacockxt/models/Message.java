package com.github.peacockxt.models;

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
        name = "message",
        indexes = {
                @Index(name = "idx_channel_message", columnList = "channel_id, message_id")
        }
)
public class Message {

    @Id
    @Column(name = "message_id")
    private Long messageId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    private String status; // DEFAULT, EDITED, DELETED

    @Column(name = "reply_to_id")
    private Long replyToId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn( name = "channel_id", nullable = false)
    private Channel channel;
}