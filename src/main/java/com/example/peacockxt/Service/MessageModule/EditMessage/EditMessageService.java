package com.example.peacockxt.Service.MessageModule.EditMessage;

import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Repository.Implimentation.MessageRepository;
import com.example.peacockxt.Service.MessageModule.CacheModule.DeleteMessageCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EditMessageService {

    private static final Logger logger = LoggerFactory.getLogger(EditMessageService.class);

    private final MessageRepository messageRepository;
    private final DeleteMessageCacheService deleteMessageCacheService;

    public EditMessageService(MessageRepository messageRepository,
                              DeleteMessageCacheService deleteMessageCacheService ){
        this.messageRepository = messageRepository;
        this.deleteMessageCacheService = deleteMessageCacheService;
    }

    /**
     * Edit a message: update DB and invalidate cache
     * @param messageId id of the message
     * @param newContent new content
     */
    public void editMessage(Long messageId, String newContent) {
        // 1. Delete cache first
        try {
            deleteMessageCacheService.deleteMessage(messageId);
        } catch (CacheAccessException e) {
            logger.warn("Cache deletion failed for messageId={}. Reason={}", messageId, e.getMessage());
            // Optional: metrics increment
        }

        // 2. Update DB
        try {
            editMessageInDatabase(messageId, newContent);
        } catch (DatabaseException e) {
            throw new BusinessLogicException("Failed to edit messageId=" + messageId, e);
        }
    }

    private void editMessageInDatabase(Long messageId, String newContent) {
        try {
            Message message = messageRepository.getMessageByMessageId(messageId);
            if (message == null) {
                throw new DatabaseException("Message not found with id=" + messageId);
            }

            message.setContent(newContent);
            message.setStatus("EDITED");
            message.setUpdateAt(LocalDateTime.now());
            messageRepository.save(message);
            logger.info("Message edited successfully, messageId={}", messageId);

        } catch (Exception e) {
            logger.error("Database error while editing messageId={}", messageId, e);
            throw new DatabaseException("Error while editing messageId=" + messageId, e);
        }
    }
}
