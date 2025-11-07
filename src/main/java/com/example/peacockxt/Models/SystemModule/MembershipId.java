package com.example.peacockxt.Models.SystemModule;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
@Embeddable
@Data
public class MembershipId implements Serializable {
    private String userId;
    private String teamId;
}
