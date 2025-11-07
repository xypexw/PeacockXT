package com.example.peacockxt.Models.SystemModule;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateSession {
    @EmbeddedId
    private PrivateSessionId privateSessionId;

    private Date createAt;
    private String status;
    private String createBy;

    @Column(unique = true)
    private String sessionId;

    @OneToMany(mappedBy = "privateSession")
    List<DirectMessage> directMessageList;
}
