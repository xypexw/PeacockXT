package com.example.peacockxt.Service.CustomModels;

import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageResponse {
    private Long messageId;
    private String content;
    private String status;
    private String createBy;
    private LocalDateTime createTime;
    private Long replyId;
    private String replyContent;
    private String replierId;
}
