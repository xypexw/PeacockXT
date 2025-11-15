package com.example.peacockxt.Service.MessageModule.ReadMessage;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Repository.Implimentation.MessageRepository;
import com.example.peacockxt.Service.CustomModels.MessageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadMessageFromDatabase {

    private final MessageRepository messageRepository;

    public ReadMessageFromDatabase(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public MessageResponse getDirectMessageResponseForCache(String channelId,Long pivotId) {
        try{
            return messageRepository.fetchMessageResponseByMessageId(pivotId,channelId);
        }
        catch(Exception e){
            throw new DatabaseException("Cannot read messages from DB", e);
        }
    }

    public List<MessageResponse> readDbMessages(String channelId , Long pivotId){
        try {
            Pageable pageable = PageRequest.of(0, 20);
            return messageRepository.fetchDirectResponse(pivotId, channelId,pageable);
        } catch (Exception e) {
            throw new DatabaseException("Cannot read messages from DB", e);
        }
    }

    public List<Long> readIndexFormDatabase( Long pivotId , String channelId ){
        try{
            Pageable pageable = PageRequest.of(0, 20);
            return messageRepository.getCurrentIndex(pivotId,channelId,pageable);
        }
        catch(Exception e){
            throw new DatabaseException("Cannot read indexes from DB", e);
        }
    }

    protected Message getMessage(Long  messageId){
        try{
            return messageRepository.getMessageByMessageId(messageId);
        }
        catch(DatabaseException e){
            throw new DatabaseException("Cannot read messages from DB", e);
        }
    }

}

