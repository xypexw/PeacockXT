package com.example.peacockxt.Controller;

import com.example.peacockxt.Annotation.Idempotent;
import com.example.peacockxt.DTO.Request.CreateRequest.CreateTeamRequest;
import com.example.peacockxt.DTO.Request.UpdateRequest.UpdateTeamRequest;
import com.example.peacockxt.DTO.Respone.TeamResponse;
import com.example.peacockxt.Service.Team.CreateTeamService;
import com.example.peacockxt.Service.Team.DeleteTeamService;
import com.example.peacockxt.Service.Team.ReadTeamService;
import com.example.peacockxt.Service.Team.UpdateTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Slf4j
public class TeamController {

    private final ReadTeamService readTeamService;
    private final CreateTeamService createTeamService;
    private final UpdateTeamService updateTeamService;
    private final DeleteTeamService deleteTeamService;

    // 1. READ TEAMS LIST VIA BULK API (GET)
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getTeams(
            @RequestParam List<Long> teamIds
    ) {
        long startTime = System.currentTimeMillis();
        List<TeamResponse> responses = readTeamService.getTeamForBulkAPI(teamIds);
        log.debug("Read bulk teams completed in {} ms", (System.currentTimeMillis() - startTime));
        return ResponseEntity.ok(responses);
    }

    // 2. CREATE NEW TEAM (POST)
    @Idempotent
    @PostMapping
    public ResponseEntity<Void> createTeam(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CreateTeamRequest request
    ) {
        createTeamService.createTeam(
                userId,
                request.description(),
                request.name(),
                request.type()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created
    }

    // 3. UPDATE TEAM (PUT)
    @Idempotent
    @PutMapping("/{teamId}")
    public ResponseEntity<Void> updateTeam(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateTeamRequest request
    ) {
        updateTeamService.updateTeamService(
                teamId,
                userId,
                request.description(),
                request.type()
        );
        return ResponseEntity.ok().build(); // 200 OK
    }

    // 4. DELETE TEAM (DELETE)
    @Idempotent
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId,
            // Receive role from BFF
            @RequestHeader("X-User-Role") String role
    ) {
        deleteTeamService.deleteTeam(
                userId,
                teamId,
                role
        );
        return ResponseEntity.ok().build(); // 200 OK
    }
}