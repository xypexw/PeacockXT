package com.example.peacockxt.DTO.Respone;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrivateSessionResponse {
    private Long privateSessionId;
    private Long partnerId;
}
