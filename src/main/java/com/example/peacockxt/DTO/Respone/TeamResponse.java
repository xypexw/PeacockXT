package com.example.peacockxt.DTO.Respone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private Long teamId;
    private String name;
    private String description;
    private String status;
    private String type;
}