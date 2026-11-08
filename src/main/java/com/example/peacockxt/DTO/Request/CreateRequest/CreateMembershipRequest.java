package com.example.peacockxt.DTO.Request.CreateRequest;

public record CreateMembershipRequest(
        Long targetUserId,
        String role
) {}