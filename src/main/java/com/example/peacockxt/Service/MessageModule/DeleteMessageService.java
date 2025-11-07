package com.example.peacockxt.Service.MessageModule;

import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Repository.Implimentation.MessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeleteMessageService {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Message> messageRedisTemplate;
    private final MessageRepository messageRepository;
    private final HelperMessageService  helperMessageService;

    public DeleteMessageService(StringRedisTemplate stringRedisTemplate
            , RedisTemplate<String, Message> messageRedisTemplate
            , MessageRepository messageRepository, HelperMessageService helperMessageService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.messageRedisTemplate = messageRedisTemplate;
        this.messageRepository = messageRepository;
        this.helperMessageService = helperMessageService;
    }

    private void deleteFromCache(String channelId , Long messageId ) {
        String messageKey = helperMessageService.getMessageKey( messageId.toString() );
        String channelKey = helperMessageService.getChannelKey( messageId.toString() );
        stringRedisTemplate.opsForZSet().remove(channelKey, messageKey);
        messageRedisTemplate.delete(messageKey);
    }

    @Transactional
    public void deleteMessage(String channelId, Long messageId) {
        deleteFromCache(channelId, messageId);
        deleteFromDatabase(messageId);
    }

    private void deleteFromDatabase(Long messageId ){
        messageRepository.deleteMessageByMessageId(messageId);
    }

}
