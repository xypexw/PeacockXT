package com.example.peacockxt.DTO.Request.CreateRequest;

public record CreateTeamRequest(
        String description,
        String name,
        String type
) {}