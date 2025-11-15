package com.example.peacockxt.Service.MessageModule.DeleteMessage;

import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Repository.Implimentation.MessageRepository;
import com.example.peacockxt.Service.MessageModule.CacheModule.DeleteMessageCacheService;
import com.example.peacockxt.Service.MessageModule.HelperMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteMessageService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteMessageService.class);

    private final MessageRepository messageRepository;
    private final DeleteMessageCacheService deleteMessageCacheService;

    public DeleteMessageService(MessageRepository messageRepository,
                                DeleteMessageCacheService deleteMessageCacheService,
                                HelperMessageService helperMessageService) {
        this.messageRepository = messageRepository;
        this.deleteMessageCacheService = deleteMessageCacheService;
    }
    @Transactional
    public void deleteMessage(String channelId, Long messageId) {
        try{
            try {
                deleteMessageCacheService.deleteMessage(messageId);
            } catch (CacheAccessException e) {
                logger.warn("Cache deletion failed for " +
                        "messageId={}, channelId={}. Reason: {}", messageId, channelId, e.getMessage());
            }
            deleteFromDatabase(messageId, channelId);
        }
        catch(DatabaseException e) {
            logger.error("Database error while deleting messageId={}, channelId={}", messageId, channelId, e);
            throw new BusinessLogicException("Message not found for messageId=" + messageId);
        }
    }

    private void deleteFromDatabase(Long messageId, String channelId) {
        try {
            logger.info("Deleting message from DB for messageId={}, channelId={}", messageId, channelId);
            messageRepository.deleteMessageByMessageId(messageId);
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete messageId=" + messageId + " from DB", e);
        }
    }
}
