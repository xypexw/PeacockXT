package com.github.peacockxt.dto.request.createrequest_temp;

public record CreateTeamRequest(
        String description,
        String name,
        String type
) {}