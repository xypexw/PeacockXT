package com.example.peacockxt.Service.MembershipService;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.SystemModule.Membership;
import com.example.peacockxt.Models.UserModule.User;
import com.example.peacockxt.Repository.Implimentation.MembershipRepository;
import com.example.peacockxt.Repository.Implimentation.UserRepository;
import com.example.peacockxt.Service.TeamModule.ReadTeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
@Service
public class CreateMembershipService {
    private static final Logger logger = LoggerFactory.getLogger(CreateMembershipService.class);
    private final ReadTeamService readTeamService;
    private final UserRepository userRepository;
    private  final MembershipRepository membershipRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final HelperMBSService helperMBSService;
    public CreateMembershipService(ReadTeamService readTeamService, UserRepository userRepository,
                                   MembershipRepository membershipRepository, StringRedisTemplate stringRedisTemplate, HelperMBSService helperMBSService) {
        this.readTeamService = readTeamService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.helperMBSService = helperMBSService;
    }

    private void saveMembership(Membership membership){
        try{
             membershipRepository.save(membership);
        }
        catch (Exception e){
            throw new DatabaseException("Database error while creating membership",e);
        }
    }

    private void deleteMembershipInCache(String userId){
        try{
            String mbsKey = helperMBSService.getUserMembershipIndexKey(userId);
            stringRedisTemplate.delete(mbsKey);
        }
        catch (Exception e){
            throw new CacheAccessException("Cache crash while creating membership",e);
        }
    }


    public Membership createMembership(String teamId, String userId) {
        try{
            Team team = readTeamService.getSingleTeam(teamId);
            User user = userRepository.getUserByUserId(userId);
            if(user == null)    throw new BusinessLogicException("User does not exist");
            if(team == null)    throw new BusinessLogicException("Team does not exist");
            Membership membership = new Membership();
            membership.setTeam(team);
            membership.setUser(user);
            saveMembership(membership);
            try{
                deleteMembershipInCache(userId);
            }
            catch(CacheAccessException e){
                logger.warn(" Cache crash teamId={} userId={}. Reason={}", teamId , userId , e.getMessage());
            }

            return membership;
        }
        catch(DatabaseException e ){
            logger.warn(" Database crash teamId={} userId={}. Reason={}", teamId , userId , e.getMessage());
            throw new BusinessLogicException("Database failure while creating membership",e);
        }
        catch(BusinessLogicException e){
            throw e;
        }
        catch (Exception e){
            throw new BusinessLogicException("logic failed " + e.getMessage() );
        }
    }

}
