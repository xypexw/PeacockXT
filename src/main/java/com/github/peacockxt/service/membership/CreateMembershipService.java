package com.github.peacockxt.service.membership;

import com.github.peacockxt.models.Membership;
import com.github.peacockxt.models.Team;
import com.github.peacockxt.models.User;
import com.github.peacockxt.models.customexception.BusinessLogicException;
import com.github.peacockxt.repository.implementation.MembershipRepository;
import com.github.peacockxt.repository.implementation.TeamRepository;
import com.github.peacockxt.repository.implementation.UserRepository;
import com.github.peacockxt.infrastructure.cache.MembershipRedisDao;
import com.github.peacockxt.service.SnowflakeIdGenerator;
import com.github.peacockxt.service.provider.TeamProvider;
import com.github.peacockxt.service.provider.UserProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class CreateMembershipService {

    private final MembershipRepository membershipRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MembershipRedisDao membershipRedisDao;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final UserProvider userProvider;
    private final TeamProvider teamProvider;

    @Transactional
    public void createMembership(Long userId, Long teamId, String role) {
        if (!teamProvider.hasTeam(teamId)) {
            throw new BusinessLogicException("Team does not exist");
        }

        if (!userProvider.hasUser(userId)) {
            throw new BusinessLogicException("User does not exist");
        }

        Membership membership = buildMembership(userId, teamId, role);
        saveMembershipToDatabase(membership);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                saveMembershipToCache(userId, teamId, membership);
            }
        });
    }

    private void saveMembershipToCache(Long userId, Long teamId, Membership membership) {
        membershipRedisDao.saveMembershipInfo(membership);
        if(membershipRedisDao.hasMembershipIndexTeamKey(teamId)){
            membershipRedisDao.addMembershipToTeam(membership.getMembershipId(), teamId);
        }
        if(membershipRedisDao.hasMembershipIndexUserKey(userId)){
            membershipRedisDao.addMembershipToUser(membership.getMembershipId(), userId);
        }
    }

    private Membership buildMembership(Long userId, Long teamId, String role) {
        Team team = teamRepository.getReferenceById(teamId);
        User user = userRepository.getReferenceById(userId);

        return Membership.builder()
                .membershipId(snowflakeIdGenerator.nextId())
                .userId(userId)
                .teamId(teamId)
                .user(user)
                .team(team)
                .joinedAt(LocalDateTime.now())
                .role(role)
                .build();
    }

    private void saveMembershipToDatabase(@NonNull Membership membership) {
        membershipRepository.save(membership);
    }
}
