package com.example.peacockxt.Service.Provider;
import com.example.peacockxt.Models.Membership;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TeamPermission {
    private final MembershipProvider membershipProviderService;
    private static final String OWNER_ROLE = "OWNER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String MEMBER_ROLE = "MEMBER";
    private static final String READER_ROLE = "READER";


    private boolean checkRole(Long membershipId, String expectedRole , Long userId) {
        Membership membership = membershipProviderService.getMembership(membershipId);
        return membership != null && expectedRole.equals(membership.getRole()) && userId.equals(membership.getUserId());
    }

    public boolean isOwner(Long membershipId , Long userId) {
        return checkRole(membershipId, OWNER_ROLE, userId);
    }
    public boolean isReader(Long membershipId, Long userId) {
        return checkRole(membershipId, READER_ROLE, userId);
    }
    public boolean isAdmin(Long membershipId, Long userId) {
        return checkRole(membershipId, ADMIN_ROLE, userId);
    }
    public boolean isMember(Long membershipId, Long userId) {
        return checkRole(membershipId, MEMBER_ROLE, userId);
    }

    public boolean isTeamAdmin(Long membershipId , Long userId ) {
        return isOwner(membershipId,userId) || isAdmin(membershipId,userId);
    }

}