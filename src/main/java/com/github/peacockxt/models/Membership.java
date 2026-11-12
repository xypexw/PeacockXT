package com.github.peacockxt.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "membership",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_membership_user_team",
                        columnNames = {"user_id", "team_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_membership_team_user",
                        columnList = "team_id,user_id"
                )
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Membership implements Serializable {

    @Id
    @Column(name = "membership_id")
    private Long membershipId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "team_id", insertable = false, updatable = false)
    private Long teamId;

    @Column(nullable = false)
    private String role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
}



