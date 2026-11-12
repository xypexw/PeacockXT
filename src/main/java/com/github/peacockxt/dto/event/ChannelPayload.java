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
public class ChannelPayload {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long channelId;

    private String name;
    private String description;
    private String status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;

    private LocalDateTime createAt;
}