package com.example.peacockxt.Service.MessageModule.CreateMessage;

import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Service.CustomModels.MessageResponse;
import com.example.peacockxt.Service.MessageModule.HelperMessageService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CreateMessageFromCache {
    private final RedisTemplate<String, MessageResponse> messageRedisTemplate ;
    private final HelperMessageService helperMessageService;

    private final int ttlDays = 5;

    public CreateMessageFromCache(RedisTemplate<String, MessageResponse> messageRedisTemplate, HelperMessageService helperMessageService) {
        this.messageRedisTemplate = messageRedisTemplate;
        this.helperMessageService = helperMessageService;
    }

    public void updateRedis(Message message, Channel channel , Message reply ) {
        try{
            String messageKey = helperMessageService.getMessageKey(message.getMessageId().toString());
            MessageResponse messageResponse = messageResponseBuilder(message,reply);
            messageRedisTemplate.opsForValue().setIfAbsent(messageKey,messageResponse,ttlDays,TimeUnit.DAYS);
        }
        catch (Exception e){
            throw new CacheAccessException("Cache Writing fail",e);
        }
    }

    private MessageResponse messageResponseBuilder(Message message , Message reply ){
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .content(message.getContent())
                .status(message.getStatus())
                .createTime(message.getCreateAt())
                .createBy(message.getCreateBy())
                .replyId(reply.getMessageId())
                .replyContent(reply.getContent())
                .replierId(reply.getCreateBy())
                .build();
    }


}
