package com.github.peacockxt.controller;

import com.github.peacockxt.annotation.Idempotent;
import com.github.peacockxt.dto.request.createrequest_temp.CreateChannelRequest;
import com.github.peacockxt.dto.request.updaterequest_temp.UpdateChannelRequest;
import com.github.peacockxt.dto.response.ChannelResponse;
import com.github.peacockxt.service.channel.CreateChannelService;
import com.github.peacockxt.service.channel.DeleteChannelService;
import com.github.peacockxt.service.channel.ReadChannelService;
import com.github.peacockxt.service.channel.UpdateChannelService;
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