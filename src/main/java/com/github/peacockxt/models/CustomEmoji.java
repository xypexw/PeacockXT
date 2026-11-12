package com.github.peacockxt.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "custom_emoji",
        indexes = {
                @Index(name = "idx_team_emoji", columnList = "team_id, emoji_name")
        }
)
public class CustomEmoji {

    @Id
    @Column(name = "emoji_id")
    private Long emojiId;

    @Column(name = "emoji_name", nullable = false , unique = true )
    private String emojiName;

    // exp : https://stogare.domain.com/bucket/team_id/emoji_name.png
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "create_by")
    private Long createBy;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'DEFAULT'")
    private String status = "DEFAULT";

}