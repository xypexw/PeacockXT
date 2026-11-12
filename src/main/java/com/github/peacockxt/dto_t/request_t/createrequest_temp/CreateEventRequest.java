package com.github.peacockxt.dto.request.createrequest_temp;
import java.time.LocalDateTime;

public record CreateEventRequest(
        String title,
        String content,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}