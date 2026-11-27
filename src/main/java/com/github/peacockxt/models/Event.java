package com.github.peacockxt.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "event",
        indexes = {
                @Index(name = "idx_event_team_id_status", columnList = "team_id, event_id, status")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String status;

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "number_attend")
    private Integer numberAttend;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "team_id", insertable = false, updatable = false)
    private Long teamId;

}

