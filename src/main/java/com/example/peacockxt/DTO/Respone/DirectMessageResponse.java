package com.example.peacockxt.DTO.Respone;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DirectMessageResponse {
    private String messageId;
    private String senderId;
    private String content;
    private LocalDateTime createAt;
    private String status; // SENT, EDITED, DELETED
    private String replyToId;
    private String replyContent;
    private String replierId;
}
