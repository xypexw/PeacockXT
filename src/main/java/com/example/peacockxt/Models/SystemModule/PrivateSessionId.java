package com.example.peacockxt.Models.SystemModule;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateSessionId implements Serializable {
    private String connectorId;
    private String receiverId;
}
