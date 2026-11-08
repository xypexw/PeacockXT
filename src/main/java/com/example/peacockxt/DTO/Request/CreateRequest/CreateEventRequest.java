package com.example.peacockxt.DTO.Request.CreateRequest;
import java.time.LocalDateTime;

public record CreateEventRequest(
        String title,
        String content,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}