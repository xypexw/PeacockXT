package com.example.peacockxt.Models.UserModule;

import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.SystemModule.Activity;
import com.example.peacockxt.Models.SystemModule.Membership;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "user",
        indexes = @Index(name = "idx_userName" , columnList = "userName" ,unique = true)
)
public class User {
    @Id
    private String userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String SystemRole;
    private String config;
    private String status;
    private LocalDateTime updateAt;
    private LocalDateTime createAt;
    private String createBy;
    private String updateBy;
    private String avatarUrl;
    private String bio;

    // relationship
    @OneToMany(mappedBy = "user")
    private List<Message> Messages;

    @OneToMany(mappedBy = "user")
    List<Activity> activities;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberships;

}
