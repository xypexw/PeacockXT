package com.example.peacockxt.Models.GroupModule.ChannelModule;

import com.example.peacockxt.Models.UserModule.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "message", indexes = @Index(name = "idx_channel_id" , columnList = "channel_id" ) )
public class Message {

    @Id
    private Long messageId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createAt;
    private String createBy;

    private LocalDateTime updateAt;
    private String updateBy;

    private String status;

    @Column(name = "reply_id", insertable = false, updatable = false)
    private String replyId;

    @Column(name = "channel_id", insertable = false, updatable = false)
    private String channelId;

    // ✅ Many-to-one relationship with Channel
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    // ✅ Self-referencing relationship for replies
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_id")
    private Message replyTo;

    // ✅ One-to-many for message replies
    @OneToMany(mappedBy = "replyTo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> replies;

    // Many to One for user
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
}


