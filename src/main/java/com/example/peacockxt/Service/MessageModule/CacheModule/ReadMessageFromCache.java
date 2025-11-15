package com.example.peacockxt.Service.MessageModule.CacheModule;

import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Service.CustomModels.MessageResponse;
import com.example.peacockxt.Service.MessageModule.HelperMessageService;
import com.example.peacockxt.Service.MessageModule.ReadMessage.ReadMessageFromDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ReadMessageFromCache {
    Logger log = LoggerFactory.getLogger(ReadMessageFromCache.class);
    private final HelperMessageService helperMessageService;
    private final RedisTemplate<String, MessageResponse> messageRedisTemplate;
    private final ReadMessageFromDatabase readMessageFromDatabase;

    public ReadMessageFromCache(HelperMessageService helperMessageService,
                                RedisTemplate<String, MessageResponse> messageRedisTemplate,
                                ReadMessageFromDatabase readMessageFromDatabase) {
        this.helperMessageService = helperMessageService;
        this.messageRedisTemplate = messageRedisTemplate;
        this.readMessageFromDatabase = readMessageFromDatabase;
    }


    public List<MessageResponse> readCacheMessage(List<Long> messageIndex , String channelId) {
        final int ttlDays = 5;
        List<MessageResponse> messages = new ArrayList<>();
        try {
            for (Long index : messageIndex) {
                String messageKey =  helperMessageService.getMessageKey(index.toString());
                MessageResponse messageResponse = messageRedisTemplate.opsForValue().get(messageKey);
                if(messageResponse == null){
                    messageResponse = readMessageFromDatabase.getDirectMessageResponseForCache(channelId,index);
                    messageRedisTemplate.opsForValue().set(messageKey,messageResponse,ttlDays, TimeUnit.DAYS);
                    log.info("Cache Miss for channelId={} MessageId={}", channelId,messageResponse.getMessageId());
                }
                else
                    log.info("Cache Hit for channelId={} MessageId={}", channelId,messageResponse.getMessageId());
                messages.add(messageResponse);
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

