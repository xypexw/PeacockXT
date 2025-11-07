package com.example.peacockxt.Service.MessageModule.CreateMessage;

import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;

import com.example.peacockxt.Repository.Implimentation.MessageRepository;
import com.example.peacockxt.Service.MessageModule.HelperMessageService;
import com.example.peacockxt.Service.SystemModule.SnowflakeIdGeneratorService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
@Service
public class CreateMessageService {

    private final MessageRepository messageRepository;
    private final SnowflakeIdGeneratorService snowflakeIdGeneratorService;
    private final CreateMessageFromCache createMessageFromCache;
    public CreateMessageService(MessageRepository messageRepository,
                                SnowflakeIdGeneratorService snowflakeIdGeneratorService,
                                CreateMessageFromCache createMessageFromCache) {
            this.messageRepository = messageRepository;
            this.snowflakeIdGeneratorService = snowflakeIdGeneratorService;
        this.createMessageFromCache = createMessageFromCache;
    }


    @Transactional
    public Message createMessage(  String content , String userId ,
                                   String channelId ,  Message reply , Channel channel ) {
        try{
            Message message = messageBuilder( content , userId , channel , reply, LocalDateTime.now() );
            saveMessageToDatabase(message);
            updateToCache(message,reply,channel);
            return message;
        }
        catch (DatabaseException e) {
            throw new BusinessLogicException("Database fall",e);
        }
        catch (CacheAccessException e) {
            throw new BusinessLogicException("Cache fall",e);
        }
        catch (Exception e) {
            throw new BusinessLogicException("Logic Fail",e);
        }
    }

    private void updateToCache(Message message , Message reply , Channel channel) {
        try{
            if(reply==null){
                Message replyDummy = messageBuilder("","",null,null,LocalDateTime.now());
                createMessageFromCache.updateRedis(message,channel,replyDummy);
            }
            else createMessageFromCache.updateRedis(message,channel,reply);
        }
        catch (CacheAccessException e) {
            throw new CacheAccessException("Cache fall",e);
        }
    }

    private Message messageBuilder(String content , String userId , Channel Channel ,
                                   Message reply , LocalDateTime timestamp) {
        final String defaultStatus = "NORMAL";
        Long messageId = snowflakeIdGeneratorService.nextId();
        return Message.builder()
                .messageId(messageId)
                .content(content)
                .createAt(timestamp)
                .createBy(userId)
                .updateAt(timestamp)
                .updateBy(userId)
                .status(defaultStatus)
                .channel(Channel)
                .replyTo(reply)
                .build();
    }

    private void saveMessageToDatabase(Message message) {
        try{
            messageRepository.save(message);
        }
        catch (Exception e){
            throw new DatabaseException("Message could not be saved in the database",e);
        }
    }

}
