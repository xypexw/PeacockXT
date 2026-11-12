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
public class PostPayload {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;

    private String title;
    private String description;
    private String status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;
}