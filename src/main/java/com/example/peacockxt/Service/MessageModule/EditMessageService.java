package com.example.peacockxt.Service.MessageModule;

import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Repository.Implimentation.MessageRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@Service
public class EditMessageService {
    private final MessageRepository messageRepository;
    private final RedisTemplate<String, Message> messageRedisTemplate ;
    private final HelperMessageService helperMessageService;
    private final String editedStatus = "EDITED";

    public EditMessageService(MessageRepository messageRepository,
                              RedisTemplate<String, String> stringRedisTemplate,
                              RedisTemplate<String, Message> messageRedisTemplate,
                              HelperMessageService helperMessageService){
        this.messageRepository = messageRepository;
        this.messageRedisTemplate = messageRedisTemplate;
        this.helperMessageService = helperMessageService;
    }

    private void editMessage(Long messageId, String content ){
        editMessageInDatabase(messageId, content);
        eraseInCache(messageId);
    }

    private void editMessageInDatabase(Long messageId, String content){
        Message edited = messageRepository.getMessageByMessageId(messageId);
        edited.setContent(content);
        edited.setStatus(editedStatus);
        edited.setUpdateAt(LocalDateTime.now());
        edited.setUpdateBy(editedStatus);
        messageRepository.save(edited);
    }

    private void eraseInCache(Long messageId) {
        String messageKey = helperMessageService.getMessageKey(messageId.toString());
        messageRedisTemplate.delete(messageKey);
    }

}
