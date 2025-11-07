package com.example.peacockxt.Models.GroupModule.PostModule.VotingModule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "choice",
        indexes = {
                @Index(name = "idx_voteId", columnList = "voting_id")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Choice {

    @Id
    @Column( name = "choice_id")
    private String ChoiceId;

    private Double percent;

    private String content;

    private String createBy;

    private LocalDateTime createAt;

    private Integer numberAttended;

    // Quan hệ ManyToOne với Voting
    @ManyToOne
    @JoinColumn(name = "voting_id", nullable = false)
    private Voting voting;
}
