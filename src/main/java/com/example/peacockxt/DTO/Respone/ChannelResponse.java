package com.example.peacockxt.DTO.Respone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponse {
    private String channelId;
    private String name;
    private String description;
    private String status;
    private Long createBy;
}