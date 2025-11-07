package com.example.peacockxt.Models.GroupModule.PostModule;

import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.GroupModule.PostModule.VotingModule.Voting;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(
        name = "post",
        indexes = {
                @Index(name = "idx_teamId_createAt",columnList = "team_id,create_at")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    private String postId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime updatedAt;
    private String updatedBy;
    @Column(name = "create_at")
    private LocalDateTime createAt;
    private String createBy;

    @ManyToOne
    @JoinColumn(name = "team_id")
    Team team;

    // quan hệ OneToMany với post
    @OneToMany(mappedBy = "post")
    private List<Comment> comments;

    // // quan hệ OneToMany với voting trong module voting
    @OneToMany(mappedBy = "post")
    private List<Voting> voting;
}
