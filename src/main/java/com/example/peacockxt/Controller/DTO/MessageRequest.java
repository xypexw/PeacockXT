package com.example.peacockxt.Controller.DTO;

import lombok.Data;

@Data
public class MessageRequest {
    private String channelId;
    private String sender;
    private String content;
    private String idempotencyKey;
    private String tokens;
    private Long replyId;
}
