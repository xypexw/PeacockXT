package com.example.peacockxt.Models.GroupModule.ChannelModule;
import com.example.peacockxt.Models.GroupModule.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(
        name = "channel",
        indexes = {
                @Index(name = "idx_TeamId_createAt", columnList = "team_id,create_at")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Channel {

    @Id
    private String channelId;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String status;

    private String createBy;
    private String updateBy;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    private LocalDateTime updateAt;

    // Quan hệ ManyToOne với Group
    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    // Quan hệ OneToMany với Message
    @OneToMany(mappedBy = "channel")
    List<Message> messages;
}
