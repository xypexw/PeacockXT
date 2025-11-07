package com.example.peacockxt.Models.SystemModule;

import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.UserModule.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "membership")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Membership implements Serializable {
    @EmbeddedId
    private MembershipId id;
    @ManyToOne
    @MapsId("userId")
    private User user;

    @ManyToOne
    @MapsId("teamId")
    private Team team;

    private String role;
}
