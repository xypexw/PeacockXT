package com.github.peacockxt.controller;
import com.github.peacockxt.annotation.Idempotent;
import com.github.peacockxt.dto.request.createrequest_temp.RefreshTokenRequest;
import com.github.peacockxt.dto.request.LoginRequest;
import com.github.peacockxt.dto.request.updaterequest_temp.UpdateAvatarRequest;
import com.github.peacockxt.dto.response.TokenResponse;
import com.github.peacockxt.service.storage.AvatarService;
import com.github.peacockxt.service.user.UserLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final UserLoginService userLoginService;
    private final AvatarService  avatarService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userLoginService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(userLoginService.refresh(request.refreshToken()));
    }

    @Idempotent
    @PostMapping("/setting-avatar")
    public ResponseEntity<String> updateAvatar(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateAvatarRequest updateAvatarRequest) {
        avatarService.updateAvatarLink(updateAvatarRequest.keyObject(), userId );
        return ResponseEntity.ok().build();
    }

    @Idempotent
    @DeleteMapping("/delete-avatar")
    public ResponseEntity<String> settingAvatar (
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-User-Id") Long userId) {
        avatarService.deleteAvatarLink(userId );
        return ResponseEntity.ok().build();
    }
}
