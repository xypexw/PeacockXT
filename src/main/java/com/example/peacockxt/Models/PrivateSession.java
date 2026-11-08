package com.example.peacockxt.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "private_session",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"connector_id", "receiver_id"})
        },
        indexes = {
                @Index(name = "idx_connector_receiver", columnList = "connector_id, receiver_id"),
                @Index(name = "idx_receiver", columnList = "receiver_id")
        }
)
public class PrivateSession {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "connector_id", nullable = false)
    private Long connectorId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    private String status;

    @Column(name = "create_by")
    private Long createBy;

}