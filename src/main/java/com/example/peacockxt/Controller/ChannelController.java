package com.example.peacockxt.Controller;

import com.example.peacockxt.Annotation.Idempotent;
import com.example.peacockxt.DTO.Request.CreateRequest.CreateChannelRequest;
import com.example.peacockxt.DTO.Request.UpdateRequest.UpdateChannelRequest;
import com.example.peacockxt.DTO.Respone.ChannelResponse;
import com.example.peacockxt.Service.Channel.CreateChannelService;
import com.example.peacockxt.Service.Channel.DeleteChannelService;
import com.example.peacockxt.Service.Channel.ReadChannelService;
import com.example.peacockxt.Service.Channel.UpdateChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final CreateChannelService createChannelService;
    private final ReadChannelService readChannelService;
    private final UpdateChannelService updateChannelService;
    private final DeleteChannelService deleteChannelService;

    @Idempotent
    @PostMapping
    public ResponseEntity<Void> createChannel(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @Valid @RequestBody CreateChannelRequest request
    ) {
        createChannelService.createChannel(request.getName(), request.getDescription(), userId, teamId, membershipId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<ChannelResponse>> getChannels(
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @RequestParam(required = false) Long cursorId
    ) {
        List<ChannelResponse> responses = readChannelService.readChannels(teamId, membershipId, userId, cursorId);
        return ResponseEntity.ok(responses);
    }

    @Idempotent
    @PutMapping("/{channelId}")
    public ResponseEntity<Void> updateChannel(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @PathVariable Long channelId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @Valid @RequestBody UpdateChannelRequest request
    ) {
        updateChannelService.updateChannel(channelId, request.getName(), request.getDescription(),
                userId, teamId, membershipId);
        return ResponseEntity.ok().build();
    }

    @Idempotent
    @DeleteMapping("/{channelId}")
    public ResponseEntity<Void> deleteChannel(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @PathVariable Long channelId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId
    ) {
        deleteChannelService.deleteChannel(channelId, userId, teamId, membershipId);
        return ResponseEntity.noContent().build();
    }
}