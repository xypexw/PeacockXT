package com.example.peacockxt.Models.GroupModule;

import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Models.GroupModule.PostModule.Post;
import com.example.peacockxt.Models.SystemModule.Membership;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    @Id
    private String teamId;

    private String name;
    private String description;

    private String status;
    private String type;

    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private String createBy;
    private String updateBy;

    @OneToMany(mappedBy = "team")
    private List<Post> posts ;

    @OneToMany(mappedBy = "team")
    private List<Channel> channels ;

    @OneToMany(mappedBy = "team")
    private List<Event> events ;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberships;

}
