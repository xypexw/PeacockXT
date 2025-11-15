package com.example.peacockxt.Service.MembershipService;

import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import com.example.peacockxt.Models.CustomException.CacheAccessException;
import com.example.peacockxt.Models.CustomException.DatabaseException;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.UserModule.User;
import com.example.peacockxt.Repository.Implimentation.MembershipRepository;
import com.example.peacockxt.Repository.Implimentation.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service class responsible for reading membership data for users.
 * Implements read-aside caching with Redis and fallback to database.
 * Read-Aside Pattern:
 * - Read from cache first.
 * - If cache miss, read from DB and populate cache.
 * - Cache Empty lists are allowed to reduce DB hits.
 * Cache Invalidation on Write:
 * - On create/update/delete membership, the corresponding Redis key should be deleted.
 */
@Service
public class ReadMembershipService {

    private static final Logger log = LoggerFactory.getLogger(ReadMembershipService.class);

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final HelperMBSService helperMBSService;

    public ReadMembershipService(StringRedisTemplate redisTemplate,
                                 UserRepository userRepository,
                                 MembershipRepository membershipRepository,
                                 HelperMBSService helperMBSService) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.helperMBSService = helperMBSService;
    }

    /**
     * Check if the Redis key for user's membership exists.
     */
    private boolean cacheKeyExists(String userId){
        try {
            String key = helperMBSService.getUserMembershipIndexKey(userId);
            return redisTemplate.hasKey(key);
        } catch (Exception e){
            log.error("Failed to check cache key for userId={}", userId, e);
            throw new CacheAccessException("Cache access failed", e);
        }
    }

    /**
     * Retrieve list of team names from Redis cache.
     */
    private List<String> getTeamIdsFromCache(String userId){
        try {
            String key = helperMBSService.getUserMembershipIndexKey(userId);
            List<String> result = redisTemplate.opsForList().range(key, 0, -1);
            log.debug("Retrieved {} teams from cache for userId={}", result != null ? result.size() : 0, userId);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e){
            log.error("Failed to read from cache for userId={}", userId, e);
            throw new CacheAccessException("Cache access failed", e);
        }
    }

    /**
     * Populate Redis cache with list of team names.
     */
    private void populateCache(String userId, List<String> teamList){
        try {
            String key = helperMBSService.getUserMembershipIndexKey(userId);
            if (!teamList.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(key, teamList);
            }
            redisTemplate.expire(key, 5, TimeUnit.DAYS); // TTL 5 days
            log.debug("Populated cache with {} teams for userId={}", teamList.size(), userId);
        } catch (Exception e){
            log.error("Failed to populate cache for userId={}", userId, e);
            throw new CacheAccessException("Cache population failed", e);
        }
    }

    /**
     * Read memberships directly from database.
     */
    private List<Team> readMembershipFromDb(String userId){
        try {
            List<Team> memberships = membershipRepository.findTeamsByUserId(userId);
            log.debug("Read {} memberships from DB for userId={}", memberships.size(), userId);
            return memberships;
        } catch (Exception e){
            log.error("Failed to read memberships from DB for userId={}", userId, e);
            throw new DatabaseException("Database read failed", e);
        }
    }

    /**
     * Implements read-aside caching logic: check cache first, then DB on miss.
     */
    private List<String> readAsideCache(String userId){
        try {
            if (cacheKeyExists(userId)) {
                log.info("Cache HIT for membership of userId={}", userId);
                return getTeamIdsFromCache(userId);
            } else {
                log.info("Cache MISS for membership of userId={}", userId);
                List<Team> memberships = readMembershipFromDb(userId);
                List<String> teamIds = convertToTeamIds(memberships);
                populateCache(userId, teamIds);
                return teamIds;
            }
        } catch(CacheAccessException e){
            log.warn("Cache error during read-aside for userId={}, falling back to DB", userId, e);
            throw e;
        } catch(DatabaseException e){
            log.error("Database error during read-aside for userId={}", userId, e);
            throw e;
        }
    }

    /**
     * Convert list of Membership objects to list of team names.
     */
    private List<String> convertToTeamIds(List<Team> teams){
        List<String> teamIds = new ArrayList<>();
        for(Team team  : teams ){
            if(team != null && team.getTeamId() != null) {
                teamIds.add(team.getTeamId());
            }
        }
        return teamIds;
    }

    /**
     * Public method to get list of team names for a user.
     * Uses read-aside caching with fallback to DB.
     */
    public List<String> getTeamIds(String userId){
        try {
            User user = userRepository.getUserByUserId(userId);
            if(user == null) {
                log.warn("User does not exist: userId={}", userId);
                throw new BusinessLogicException("User does not exist");
            }
        // Add validation for user existence
            try {
                return readAsideCache(userId);
            } catch(CacheAccessException e){
                log.warn("Cache failed during read-aside for userId={}, fallback to DB", userId, e);
            }

            // Fallback: read directly from DB if cache fails
            List<String> teamIds = convertToTeamIds(readMembershipFromDb(userId));
            log.info("Returning {} teams from DB for userId={} due to cache failure", teamIds.size(), userId);
            return teamIds;

        } catch(DatabaseException e){
            log.error("Database error while getting team list for userId={}", userId, e);
            throw new BusinessLogicException("Database error while getting team list", e);
        }
    }
}