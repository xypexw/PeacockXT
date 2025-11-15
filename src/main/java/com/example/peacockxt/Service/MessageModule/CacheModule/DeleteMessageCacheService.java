package com.example.peacockxt.Service.MessageModule.CacheModule;

import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Service.CustomModels.MessageResponse;
import com.example.peacockxt.Service.MessageModule.HelperMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeleteMessageCacheService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteMessageCacheService.class);

    private final RedisTemplate<String, MessageResponse> messageResponseRedisTemplate;
    private final HelperMessageService helperMessageService;

    public DeleteMessageCacheService(RedisTemplate<String, MessageResponse> messageResponseRedisTemplate,
                                     HelperMessageService helperMessageService) {
        this.messageResponseRedisTemplate = messageResponseRedisTemplate;
        this.helperMessageService = helperMessageService;
    }

    private String buildMessageResponseKey(Long messageId) {
        return helperMessageService.getMessageKey(messageId.toString());
    }

    //Delete message from cache
    public void deleteMessage(Long messageId) {
        try {
            String key = buildMessageResponseKey(messageId);
            logger.info("Deleting message from cache for messageId={}", messageId);
            messageResponseRedisTemplate.delete(key);
        } catch (Exception e) {
            throw new CacheAccessException("Cache crash while deleting messageId: " + messageId, e);
        }
    }


}
