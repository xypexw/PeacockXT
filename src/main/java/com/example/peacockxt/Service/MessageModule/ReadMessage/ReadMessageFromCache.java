package com.example.peacockxt.Service.MessageModule.ReadMessage;

import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Repository.Implimentation.MessageRepository;
import com.example.peacockxt.Service.MessageModule.HelperMessageService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ReadMessageFromCache {

    private final HelperMessageService helperMessageService;
    private final RedisTemplate<String, Message> messageRedisTemplate;
    private final MessageRepository messageRepository;

    public ReadMessageFromCache(HelperMessageService helperMessageService,
                                RedisTemplate<String, Message> messageRedisTemplate,
                                MessageRepository messageRepository,
                                RedisTemplate<String, String> stringRedisTemplate) {
        this.helperMessageService = helperMessageService;
        this.messageRedisTemplate = messageRedisTemplate;
        this.messageRepository = messageRepository;
    }


    public List<Message> readCacheMessage(List<String> messageIndex) {
        final int ttlDays = 5;
        List<Message> messages = new ArrayList<>();
        try {
            for (String index : messageIndex) {
                String messageKey = helperMessageService.getMessageKey(index);
                Message message = messageRedisTemplate.opsForValue().get(messageKey);

                if (message == null) {
                    Long messageId = helperMessageService.parseStringToLong(index);
                    message = messageRepository.getMessageByMessageId(messageId);
                    messageRedisTemplate.opsForValue().set(messageKey, message, ttlDays, TimeUnit.DAYS);
                } else {
                    messages.add(message);
                }
            }
            if (messages.isEmpty()) {
                throw new CacheAccessException("No messages found");
            } else {
                return messages;
            }
        }
        catch (DatabaseException e) {
            throw new CacheAccessException("No messages found form Database ",e);
        }
        catch(Exception e) {
            throw new CacheAccessException("No messages found form Cache ",e);
        }
    }

}

