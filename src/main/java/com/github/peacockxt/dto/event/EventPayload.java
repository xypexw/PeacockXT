package com.github.peacockxt.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPayload {
    private Long eventId;
    private String title;
    private String content;
    private String status;
    private Long createBy;
    private LocalDateTime createAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberAttend;
    private Long teamId;
}