package com.example.peacockxt.Models.SystemModule;
import com.example.peacockxt.Models.UserModule.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "activity",
        indexes = { @Index( name = "idx_userId_timeStamp " , columnList = "user_id,time_stamp" , unique = false ) }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    @Id
    private String activityId;
    private String title;
    private String content;
    private String activityCode;
    private String status;
    @Column(name = "time_stamp")
    private LocalDateTime timeStamp;

    // relationship

    @ManyToOne
    @JoinColumn(name = "user_id" , nullable = false , unique = false )
    private User user;

}
