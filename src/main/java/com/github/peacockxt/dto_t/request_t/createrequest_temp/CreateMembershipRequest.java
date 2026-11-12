package com.github.peacockxt.dto.request.createrequest_temp;

public record CreateMembershipRequest(
        Long targetUserId,
        String role
) {}