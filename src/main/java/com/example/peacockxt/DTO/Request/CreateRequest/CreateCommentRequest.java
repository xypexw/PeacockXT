package com.example.peacockxt.DTO.Request.CreateRequest;
public record CreateCommentRequest(
        String content,
        Long replyToId
) {}