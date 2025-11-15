package com.example.peacockxt.Service.MembershipService;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class HelperMBSService {
    //Return the Redis key for user's membership cache.
    public String getUserMembershipIndexKey(String userId) {
        String membershipIndexCacheKey = "MBS::";
        return membershipIndexCacheKey + userId;
    }
}
