package com.example.peacockxt.Controller.DTO;

import lombok.Data;

@Data
public class TeamResquest {
    String idempotencyKey;
    String tokens;
    String userId;
}
