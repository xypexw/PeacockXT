package com.example.peacockxt.Models.GroupModule.PostModule.VotingModule;

import com.example.peacockxt.Models.GroupModule.PostModule.Post;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data
@Table(
        name = "voting",
        indexes = {
                @Index( name = "idx_postId" , columnList = "post_id")
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voting {

    @Id
    private String voteId;
    private String title;
    private String content;

    private String status;

    private String createBy;
    private String updateBy;

    private Date createAt;
    private Date updateAt;

    private int numberAttended;

    @ManyToOne
    @JoinColumn(name = "post_id" )
    private Post post;

    @OneToMany(mappedBy = "voting")
    private List<Choice> choice;

}
