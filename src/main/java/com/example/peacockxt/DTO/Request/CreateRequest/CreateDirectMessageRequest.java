package com.example.peacockxt.DTO.Request.CreateRequest;
public record CreateDirectMessageRequest(
        String content,
        Long replyToId
) {}