package com.example.peacockxt.DTO.Request.CreateRequest;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateEmojiRequest {

    @NotBlank(message = "Emoji name must not be blank")
    private String emojiName;

    @NotBlank(message = "Object key must not be blank")
    private String objectKey;
}