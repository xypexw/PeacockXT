package com.example.peacockxt.Controller;

import com.example.peacockxt.Annotation.Idempotent;
import com.example.peacockxt.DTO.Request.CreateRequest.CreateEventRequest;
import com.example.peacockxt.DTO.Respone.EventResponse;
import com.example.peacockxt.Service.Event.CreateEventService;
import com.example.peacockxt.Service.Event.DeleteEventService;
import com.example.peacockxt.Service.Event.ReadEventService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/events")
@RequiredArgsConstructor
public class EventController {

    private final ReadEventService readEventService;
    private final CreateEventService createEventService;
    private final DeleteEventService deleteEventService;

    // 1. READ EVENTS LIST (GET)
    // URL: GET /api/v1/teams/10/events?cursorId=5
    @GetMapping
    public ResponseEntity<List<EventResponse>> readEvents(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long cursorId
    ) {
        List<EventResponse> events = readEventService.readEvent(teamId, cursorId);

        return ResponseEntity.ok(events);
    }

    // 2. CREATE EVENT (POST)
    // URL: POST /api/v1/teams/10/events
    @Idempotent
    @PostMapping
    public ResponseEntity<Void> createEvent(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId, // Acts as creatorId passed down from BFF
            @RequestBody CreateEventRequest request
    ) {
        createEventService.createEvent(
                request.title(),
                request.content(),
                userId,
                request.startTime(),
                request.endTime(),
                teamId
        );
        return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created
    }

    // 3. DELETE EVENT (DELETE)
    // URL: DELETE /api/v1/teams/10/events/99
    @Idempotent
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,   // Extracted from URL
            @PathVariable Long eventId,
            // Future : Add this Header later to check if the user has permission to delete
            @RequestHeader("X-User-Id") Long userId
    ) {
        deleteEventService.deleteEvent(teamId, eventId);
        return ResponseEntity.ok().build(); // 200 OK
    }
}