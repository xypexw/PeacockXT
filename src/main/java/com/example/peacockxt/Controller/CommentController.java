package com.example.peacockxt.Controller;

import com.example.peacockxt.Annotation.Idempotent;
import com.example.peacockxt.DTO.Request.CreateRequest.CreateCommentRequest;
import com.example.peacockxt.DTO.Request.UpdateRequest.UpdateCommentRequest;
import com.example.peacockxt.DTO.Respone.CommentResponse;
import com.example.peacockxt.Service.Comment.CreateCommentService;
import com.example.peacockxt.Service.Comment.DeleteCommentService;
import com.example.peacockxt.Service.Comment.ReadCommentService;
import com.example.peacockxt.Service.Comment.UpdateCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final ReadCommentService readCommentService;
    private final CreateCommentService createCommentService;
    private final UpdateCommentService updateCommentService;
    private final DeleteCommentService deleteCommentService;

    // 1. READ COMMENTS (GET)
    @GetMapping
    public ResponseEntity<List<CommentResponse>> readComments(
            @PathVariable Long postId,
            @RequestParam(required = false) Long cursorId,
            @RequestHeader("X-Team-Id") Long teamId,           // teamId is assigned from BFF via API Aggregate
            @RequestHeader("X-User-Id") Long userId,           // Decoded from token and passed down by API Gateway
            @RequestHeader("X-Membership-Id") Long membershipId
    ) {
        long startTime = System.currentTimeMillis();
        List<CommentResponse> responses = readCommentService.readComment(postId, teamId, userId, membershipId, cursorId);
        log.debug("Read comments completed in {} ms", (System.currentTimeMillis() - startTime));
        return ResponseEntity.ok(responses);
    }

    // 2. CREATE COMMENT (POST)
    @Idempotent
    @PostMapping
    public ResponseEntity<Void> createComment(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @RequestBody CreateCommentRequest request
    ) {
        createCommentService.createComment(
                userId,
                membershipId,
                postId,
                request.content(),
                request.replyToId()
        );
        // POST returns 201 CREATED
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 3. UPDATE COMMENT (PUT)
    @Idempotent
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @RequestBody UpdateCommentRequest request
    ) {
        updateCommentService.updateComment(
                userId,
                membershipId,
                commentId,
                request.content(),
                postId
        );
        // Returns HTTP 200 (or 204 No Content) REST standard
        return ResponseEntity.ok().build();
    }

    // 4. DELETE COMMENT (DELETE)
    @Idempotent
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long postId, // kept hierarchical URL for high consistency even if unused
            @PathVariable Long commentId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId
    ) {
        deleteCommentService.deleteComment(
                userId,
                membershipId,
                commentId
        );
        return ResponseEntity.ok().build();
    }
}