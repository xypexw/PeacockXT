package com.example.peacockxt.Service.MembershipService;

import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Repository.Implimentation.MembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeleteMembershipService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteMembershipService.class);

    private final HelperMBSService helperMBSService;
    private final MembershipRepository membershipRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public DeleteMembershipService(MembershipRepository membershipRepository,
                                   HelperMBSService helperMBSService,
                                   StringRedisTemplate stringRedisTemplate) {
        this.membershipRepository = membershipRepository;
        this.helperMBSService = helperMBSService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Delete the user's membership cache in Redis
     */
    private void deleteInCache(String userId) {
        try {
            String key = helperMBSService.getUserMembershipIndexKey(userId);
            stringRedisTemplate.delete(key);
            logger.info("Deleted cache for userId={}", userId);
        } catch (Exception e) {
            logger.error("Failed to delete cache for userId={}", userId, e);
            throw new CacheAccessException("Cache deletion failed for userId=" + userId, e);
        }
    }

    /**
     * Delete the membership record in the database
     */
    private void deleteInDatabase(String userId, String teamId) {
        try {
            membershipRepository.deleteMemberShipUserIdAndTeamId(userId, teamId);
            logger.info("Deleted membership in DB for userId={}, teamId={}", userId, teamId);
        } catch (Exception e) {
            logger.error("Failed to delete membership in DB for userId={}, teamId={}", userId, teamId, e);
            throw new DatabaseException("Database deletion failed for userId=" + userId + ", teamId=" + teamId, e);
        }
    }

    /**
     * Delete membership: remove from cache first, then from database
     */
    public void deleteMembership(String userId, String teamId) {
        try {
            deleteInCache(userId);
            deleteInDatabase(userId, teamId);
        } catch (CacheAccessException | DatabaseException e) {
            // Wrap specific exceptions into business logic exception
            throw new BusinessLogicException("Failed to delete membership for userId=" + userId + ", teamId=" + teamId, e);
        } catch (Exception e) {
            throw new BusinessLogicException("Unexpected error while deleting membership for userId=" + userId + ", teamId=" + teamId, e);
        }
    }
}
