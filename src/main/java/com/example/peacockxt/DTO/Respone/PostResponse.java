package com.example.peacockxt.DTO.Respone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
    private String postId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private String createBy;
}