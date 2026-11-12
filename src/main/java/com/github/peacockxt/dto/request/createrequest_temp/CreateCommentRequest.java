package com.github.peacockxt.dto.request.createrequest_temp;
public record CreateCommentRequest(
        String content,
        Long replyToId
) {}