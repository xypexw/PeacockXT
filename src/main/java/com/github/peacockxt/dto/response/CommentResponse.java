package com.github.peacockxt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private String commentId;
    private String senderId;
    private String content;
    private LocalDateTime createdAt;
    private String status;
    private String replyToId;
    private String replyContent;
    private String replierId;
}