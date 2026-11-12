package com.github.peacockxt.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MessageResponse {
    private String messageId;
    private String senderId;
    private String content;
    private LocalDateTime createAt;
    private String status; // SENT, EDITED, DELETED
    private String replyToId;
    private String replyContent;
    private String replierId;
}
