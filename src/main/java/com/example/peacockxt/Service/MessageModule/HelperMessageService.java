package com.example.peacockxt.Service.MessageModule;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class HelperMessageService {
    private final String messageKeyPrefix = "MESSAGE:";
    private final String channelKeyPrefix = "CHANNEL:";
    private final String channelKeyPostfix = "MS";
    public Long parseStringToLong(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    public String getMessageKey(String messageId){
        return messageKeyPrefix + messageId;
    }


}
