package com.example.peacockxt.Controller.DTO;

import lombok.Data;

@Data
public class UserRequest {
    private String jwtToken;
    private String idempotencyKey;
}
