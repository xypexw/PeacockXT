package com.example.peacockxt.DTO.Respone;

import com.example.peacockxt.Models.Membership;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipResponse {
    private Long membershipId;
    private Long userId;
    private String role;
}
