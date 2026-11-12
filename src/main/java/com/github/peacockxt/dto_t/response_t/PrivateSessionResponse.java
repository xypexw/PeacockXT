package com.github.peacockxt.dto.response;

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
