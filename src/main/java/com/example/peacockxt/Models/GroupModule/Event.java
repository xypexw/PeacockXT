package com.example.peacockxt.Models.GroupModule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "event",
        indexes = {
                @Index(name = "idx_teamId_createAt", columnList = "team_id,create_at")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    private String eventId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String status;

    private String createBy;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    private Integer numberAttend;

    // Quan hệ N-1: Một Group có nhiều Event
    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
}
