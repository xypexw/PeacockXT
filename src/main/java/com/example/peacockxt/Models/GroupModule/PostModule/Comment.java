package com.example.peacockxt.Models.GroupModule.PostModule;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "comment",
        indexes = {
                @Index( name = "idx_postId_createDate", columnList = "post_id,create_date" )
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    private Long commentId;
    private String content;
    private String status;

    private String createBy;
    private String updateBy;
    @Column(name = "create_date")
    private LocalDateTime createDate;
    private LocalDateTime updateDate;

    // Quan hệ ManyToOne với comment ( recursive )
    @ManyToOne
    @JoinColumn(name = "reply_id" , nullable = true )
    private Comment comment;

    // Quan hệ ManyToOne với comment
    @ManyToOne
    @JoinColumn(name = "post_id" , nullable = false )
    private Post post;
}
