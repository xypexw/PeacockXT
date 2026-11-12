package com.github.peacockxt.controller;

import com.github.peacockxt.annotation.Idempotent;
import com.github.peacockxt.dto.response.PostResponse;
import com.github.peacockxt.service.post.CreatePostService;
import com.github.peacockxt.service.post.DeletePostService;
import com.github.peacockxt.service.post.ReadPostService;
import com.github.peacockxt.service.post.UpdatePostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final ReadPostService readPostService;
    private final CreatePostService createPostService;
    private final UpdatePostService updatePostService;
    private final DeletePostService deletePostService;

    // --- DTO Requests:
    public record CreatePostRequest(String title, String description) {}
    public record UpdatePostRequest(String title, String description) {}

    // 1. READ POST LIST (GET)
    // URL: GET /api/v1/teams/10/posts?cursorPostId=5
    @GetMapping
    public ResponseEntity<List<PostResponse>> getPosts(
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @RequestParam(required = false) Long cursorPostId
    ) {
        long startTime = System.currentTimeMillis();
        List<PostResponse> responses = readPostService.getPosts(userId, membershipId, teamId, cursorPostId);
        log.debug("Read posts list completed in {} ms", (System.currentTimeMillis() - startTime));
        return ResponseEntity.ok(responses);
    }

    // 2. READ POST DETAIL (GET)
    // URL: GET /api/v1/teams/10/posts/99
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostDetail(
            @PathVariable Long teamId,
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId
    ) {
        long startTime = System.currentTimeMillis();
        PostResponse response = readPostService.getPostDetail(userId, membershipId, teamId, postId);
        log.debug("Read post detail completed in {} ms", (System.currentTimeMillis() - startTime));
        return ResponseEntity.ok(response);
    }

    // 3. CREATE NEW POST (POST)
    // URL: POST /api/v1/teams/10/posts
    @Idempotent
    @PostMapping
    public ResponseEntity<Void> createPost(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @RequestBody CreatePostRequest request
    ) {
        createPostService.createPost(
                userId,
                teamId,
                membershipId,
                request.title(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created
    }

    // 4. UPDATE POST (PUT)
    // URL: PUT /api/v1/teams/10/posts/99
    @Idempotent
    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Long updaterId,
            @RequestHeader("X-Membership-Id") Long membershipId,
            @RequestBody UpdatePostRequest request
    ) {
        updatePostService.updatePost(
                postId,
                updaterId,
                request.title(),
                request.description(),
                membershipId,
                teamId
        );
        return ResponseEntity.ok().build(); // 200 OK
    }

    // 5. DELETE POST (DELETE)
    // URL: DELETE /api/v1/teams/10/posts/99
    @Idempotent
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @PathVariable Long teamId,
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") Long updaterId,
            @RequestHeader("X-Membership-Id") Long membershipId
    ) {
        deletePostService.deletePost(
                postId,
                updaterId,
                membershipId,
                teamId
        );
        return ResponseEntity.ok().build(); // 200 OK
    }
}