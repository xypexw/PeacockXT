package com.github.peacockxt.dto.request.createrequest_temp;
public record CreateDirectMessageRequest(
        String content,
        Long replyToId
) {}