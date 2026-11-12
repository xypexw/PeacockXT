package com.github.peacockxt.dto.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessagePayload {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;

    private String content;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private String status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long replyToId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long channelId;
}