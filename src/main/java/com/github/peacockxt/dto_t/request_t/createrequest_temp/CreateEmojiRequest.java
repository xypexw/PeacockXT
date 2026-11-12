package com.github.peacockxt.dto.request.createrequest_temp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateEmojiRequest {

    @NotBlank(message = "Emoji name must not be blank")
    private String emojiName;

    @NotBlank(message = "Object key must not be blank")
    private String objectKey;
}