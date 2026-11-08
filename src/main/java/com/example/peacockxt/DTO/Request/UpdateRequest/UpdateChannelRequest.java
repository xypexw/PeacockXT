package com.example.peacockxt.DTO.Request.UpdateRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChannelRequest {
    @NotBlank(message = "Channel name cannot be blank")
    private String name;

    private String description;
}