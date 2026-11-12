package com.github.peacockxt.controller;

import com.github.peacockxt.annotation.Idempotent;
import com.github.peacockxt.dto.request.createrequest_temp.CreateMembershipRequest;
import com.github.peacockxt.dto.request.updaterequest_temp.UpdateMembershipRequest;
import com.github.peacockxt.dto.response.MembershipResponse;
import com.github.peacockxt.service.membership.CreateMembershipService;
import com.github.peacockxt.service.membership.DeleteMembershipService;
import com.github.peacockxt.service.membership.ReadMembershipService;
import com.github.peacockxt.service.membership.UpdateMembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// Set base path to /api/v1 to allow flexible routing for both Team and User endpoints
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class MembershipController {

    private final CreateMembershipService createMembershipService;
    private final ReadMembershipService readMembershipService;
    private final UpdateMembershipService updateMembershipService;
    private final DeleteMembershipService deleteMembershipService;

    // 1. READ APIs (GET)

    // 1.1 Retrieve all memberships within a specific team
    // Endpoint: GET /api/v1/teams/{teamId}/memberships
    @GetMapping("/teams/{teamId}/memberships")
    public ResponseEntity<List<MembershipResponse>> getTeamMemberships(
            @PathVariable Long teamId
    ) {
        long startTime = System.currentTimeMillis();
        List<MembershipResponse> responses = readMembershipService.getTeamMemberships(teamId);
        log.debug("Fetched team memberships in {} ms", (System.currentTimeMillis() - startTime));
        return ResponseEntity.ok(responses);
    }

    // 1.2 Retrieve all memberships associated with the authenticated user
    // Endpoint: GET /api/v1/users/me/memberships
    @GetMapping("/users/me/memberships")
    public ResponseEntity<List<MembershipResponse>> getMyMemberships(
            @RequestHeader("X-User-Id") Long userId
    ) {
        long startTime = System.currentTimeMillis();
        List<MembershipResponse> responses = readMembershipService.getMemberMemberships(userId);
        log.debug("Fetched user memberships in {} ms", (System.currentTimeMillis() - startTime));
        return ResponseEntity.ok(responses);
    }

    // 1.3 Retrieve the authenticated user's specific membership details within a given team
    // Endpoint: GET /api/v1/teams/{teamId}/memberships/me
    @GetMapping("/teams/{teamId}/memberships/me")
    public ResponseEntity<MembershipResponse> getMyMembershipInTeam(
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        MembershipResponse response = readMembershipService.getSingleMembership(userId, teamId);
        return ResponseEntity.ok(response);
    }

    // ===================================================================================
    // 2. WRITE APIs (POST, PUT, DELETE)
    // ===================================================================================

    // 2.1 Add a new member to a team (Admin action)
    // Endpoint: POST /api/v1/teams/{teamId}/memberships
    @Idempotent
    @PostMapping("/teams/{teamId}/memberships")
    public ResponseEntity<Void> addMemberToTeam(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long adminUserId,
            @RequestBody CreateMembershipRequest request
    ) {
        // Note: Currently createMembership does not accept requester ID, but consider passing
        // adminUserId in the future to enforce role-based access control (RBAC).
        createMembershipService.createMembership(
                request.targetUserId(),
                teamId,
                request.role()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 2.2 Update a member's role (e.g., promote to Admin)
    // Endpoint: PUT /api/v1/teams/{teamId}/memberships/{membershipId}
    @Idempotent
    @PutMapping("/teams/{teamId}/memberships/{membershipId}")
    public ResponseEntity<Void> updateMembershipRole(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @PathVariable Long membershipId, // Target membership ID to update
            @RequestHeader("X-Membership-Id") Long updaterMembershipId, // Requester's membership ID (Admin)
            @RequestHeader("X-User-Id") Long updaterId, // Requester's user ID
            @RequestBody UpdateMembershipRequest request
    ) {
        updateMembershipService.updateMembership(
                membershipId,
                updaterMembershipId,
                request.role(),
                updaterId
        );
        return ResponseEntity.ok().build();
    }

    // 2.3 Remove a member from a team or leave the team
    // Endpoint: DELETE /api/v1/teams/{teamId}/memberships/{membershipId}
    @Idempotent
    @DeleteMapping("/teams/{teamId}/memberships/{membershipId}")
    public ResponseEntity<Void> deleteMembership(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @PathVariable Long membershipId, // Target membership ID to delete
            @RequestHeader("X-Membership-Id") Long updaterMembershipId, // Requester's membership ID
            @RequestHeader("X-User-Id") Long updaterId // Requester's user ID
    ) {
        deleteMembershipService.deleteMembership(
                membershipId,
                updaterMembershipId,
                updaterId
        );
        return ResponseEntity.ok().build();
    }
}