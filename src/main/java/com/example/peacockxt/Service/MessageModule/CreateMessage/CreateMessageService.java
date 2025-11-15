package com.example.peacockxt.Service.MessageModule.CreateMessage;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Message;
import com.example.peacockxt.Models.UserModule.User;
import com.example.peacockxt.Repository.Implimentation.MessageRepository;
import com.example.peacockxt.Repository.Implimentation.UserRepository;
import com.example.peacockxt.Service.ChannelModule.ReadChannelService;
import com.example.peacockxt.Service.MessageModule.ReadMessage.ReadMessageService;
import com.example.peacockxt.Service.SystemModule.SnowflakeIdGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/*** Service responsible for the business logic of creating new messages.
 * This includes ID generation (Snowflake), input validation, saving to the database,
 * and finally, updating the cache (Redis) (Write-Through Strategy ).
 * This serves as the main entry-point for any "create message" action in the system.*/

@Service
public class CreateMessageService {
    private static final Logger log = LoggerFactory.getLogger(CreateMessageService.class);
    private final MessageRepository messageRepository;
    private final SnowflakeIdGeneratorService snowflakeIdGeneratorService;
    private final CreateMessageFromCache createMessageFromCache;
    private final ReadMessageService readMessageService;
    private final ReadChannelService readChannelService;
    private final UserRepository userRepository;

    /**
     * Constructs the service and injects all required dependencies.
     *
     * @param messageRepository         Repository for Message DB interactions.
     * @param snowflakeIdGeneratorService Service for generating unique Snowflake IDs.
     * @param createMessageFromCache    Sub-service to handle cache update logic.
     * @param readMessageService        Service to read messages (for fetching replies).
     * @param readChannelService        Service to read channels.
     * @param userRepository            Repository to fetch user information.
     */

    public CreateMessageService(MessageRepository messageRepository,
                                SnowflakeIdGeneratorService snowflakeIdGeneratorService,
                                CreateMessageFromCache createMessageFromCache, ReadMessageService readMessageService, ReadChannelService readChannelService, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.snowflakeIdGeneratorService = snowflakeIdGeneratorService;
        this.createMessageFromCache = createMessageFromCache;
        this.readMessageService = readMessageService;
        this.readChannelService = readChannelService;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new message, saves it to the database, and updates the cache.
     * <p>
     * This entire operation is wrapped in a transaction (@Transactional).
     * If any step (DB save, Cache update) fails, the entire operation
     * will be rolled back.
     *
     * @param content   The text content of the message.
     * @param userId    The ID of the user sending the message.
     * @param channelId The ID of the channel where the message is being sent.
     * @param replyId   The ID of the message being replied to (can be null).
     * @return The newly created and persisted Message object.
     * @throws BusinessLogicException If a business rule fails (e.g., user/channel not found)
     * or if a technical exception (DB/Cache) is wrapped.
     */

    @Transactional
    public Message createMessage(  String content , String userId ,
                                   String channelId ,  Long replyId ) {
        try {
            Message reply = readMessageService.getMessage(replyId);
            Channel channel = readChannelService.readSingleChannel(channelId);
            User user = userRepository.getUserByUserId(userId);
            if(channel==null) throw new BusinessLogicException("channel is not exist");
            if(user==null)  throw new BusinessLogicException("user is not exist");
            Message message = messageBuilder( content , user , channel , reply, LocalDateTime.now() );
            saveMessageToDatabase(message);
            updateToCache(message,reply,channel);
            log.info("Message {} created successfully by user {} in channel {}", message.getMessageId(), userId, channelId);
            return message;
        }
        catch (DatabaseException e) {
            log.error("Database failed for user {} in channel {}", userId, channelId, e);
            throw new BusinessLogicException(" Business Database fall in createMessage",e);
        }
        catch (CacheAccessException e) {
            log.error("Cache failed for user {} in channel {}", userId, channelId, e);
            throw new BusinessLogicException(" Business Cache fall in createMessage",e);
        }
        catch (Exception e) {
            log.error("UNKNOWN failure while creating message for user {} in channel {}", userId, channelId, e);
            throw new BusinessLogicException("Logic Fail in createMessage",e);
        }
    }

    /**
     * Private helper to encapsulate the cache update logic.
     * <p>
     * This is separated to keep createMessage() cleaner and handle
     * the {@link CacheAccessException} specifically.
     *
     * @param message The main message that was just created.
     * @param reply   The message being replied to (if any).
     * @param channel The channel containing the message.
     * @throws CacheAccessException If the cache service (Redis) fails.
     */

    private void updateToCache(Message message , Message reply , Channel channel) {
        try{
            createMessageFromCache.updateRedis(message,channel,reply);
        }
        catch (CacheAccessException e) {
            throw new CacheAccessException("Cache fall in createMessage",e);
        }
    }

    /**
     * Private factory method to build the Message object.
     * <p>
     * Encapsulates ID generation, setting default values, and handling the nullable reply logic.
     *
     * @param content   The message content.
     * @param user      The User entity.
     * @param channel   The Channel entity.
     * @param reply     The Message entity being replied to (importantly, this can be {@code null}).
     * @param timestamp The creation timestamp.
     * @return A fully constructed Message object, ready to be persisted.
     */

    private Message messageBuilder(String content, User user, Channel channel,
                                   Message reply, LocalDateTime timestamp) {
        final String defaultStatus = "NORMAL";
        Long messageId = snowflakeIdGeneratorService.nextId();
        Message.MessageBuilder builder = Message.builder()
                .messageId(messageId)
                .content(content)
                .createAt(timestamp)
                .createBy(user.getUserId())
                .updateAt(timestamp)
                .status(defaultStatus)
                .channel(channel)
                .user(user);
        if (reply != null) {
            builder.replyTo(reply);
        }
        return builder.build();
    }

    /**
     * Private helper to encapsulate the database save logic.
     * <p>
     * Its main purpose is to catch any generic {@link Exception} from the repository
     * and wrap it into a more specific {@link DatabaseException}.
     *
     * @param message The Message object to save.
     * @throws DatabaseException If the repository (JPA/Hibernate) throws an error during save.
     */

    private void saveMessageToDatabase(Message message) {
        try{
            messageRepository.save(message);
        }
        catch (Exception e){
            throw new DatabaseException("Message could not be saved in the database",e);
        }
    }

}
