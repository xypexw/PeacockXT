package com.example.peacockxt.Service.ChannelModule;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.ChannelModule.Channel;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Repository.Implimentation.ChannelRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class CreatingChannelService {
    private final Logger logger = LoggerFactory.getLogger(CreatingChannelService.class);
    private final RedisTemplate<String, Channel> channelRedisTemplate;
    private final ChannelRepository channelRepository;
    private final HelperChannelService helperChannelService;
    public CreatingChannelService(RedisTemplate<String, Channel> channelRedisTemplate ,
                                  ChannelRepository channelRepository,
                                  HelperChannelService helperChannelService) {
        this.channelRedisTemplate = channelRedisTemplate;
        this.channelRepository = channelRepository;
        this.helperChannelService = helperChannelService;
    }

    @Transactional
    public void CreatingChannel(String name , String description , String userId , String teamId) {
        String channelId = UuidCreator.getTimeBased().toString();
        try{
            Team team = helperChannelService.getTeam(teamId);
            if(team==null){
                throw new BusinessLogicException("Team is not exist");
            }
            Channel channel = channelBuilder(channelId,name,description,userId,team);
            try{
                deleteKeyInCache(team);
            }
            catch(CacheAccessException e){
                logger.error("Cache crash when creating the channel {} and cause by {} ",channelId,e.getMessage());
            }
            saveChannelInDatabase(channel);
            logger.info("Channel {} created successfully by user {} in team {}", channelId, userId, teamId);
        }

        catch(DatabaseException e){
            logger.error("database crash when creating the channel {} and cause by {} "  , channelId , e.getMessage() );
            throw new BusinessLogicException("Database fail cause by " , e);
        }
        catch (BusinessLogicException e){
            logger.error("Database crash when creating the channel :{} and cause by {} " , channelId , e.getMessage() );
            throw new BusinessLogicException("Unknow fail  " , e);
        }
    }

    private Channel channelBuilder( String channelId , String name , String description , String userId , Team team){
        String defaultStatus = "Default";
        return Channel.builder()
                .channelId(channelId)
                .name(name)
                .description(description)
                .createBy(userId)
                .updateBy(userId)
                .createAt(LocalDateTime.now())
                .updateAt(LocalDateTime.now())
                .status(defaultStatus)
                .team(team)
                .build();
    }

    private void saveChannelInDatabase(Channel channel){
        try{
            channelRepository.save(channel);
        }
        catch(Exception e){
            throw new DatabaseException("Database fail in creating channel ", e);
        }
    }

    private void deleteKeyInCache(Team team){
        try{
            String teamKey = helperChannelService.getTeamKeyPrefix(team.getTeamId());
            channelRedisTemplate.delete(teamKey);
        }
        catch(Exception e){
            throw new CacheAccessException("Cache fail in creating channel ", e);
        }
    }


}
