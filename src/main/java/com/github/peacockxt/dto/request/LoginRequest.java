package com.github.peacockxt.dto.request;
public record LoginRequest(
        String username,
        String password
) {
}