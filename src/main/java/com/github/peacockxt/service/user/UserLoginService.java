package com.github.peacockxt.service.user;

import com.github.peacockxt.dto.request.LoginRequest;
import com.github.peacockxt.dto.response.TokenResponse;
import com.github.peacockxt.infrastructure.cache.UserSessionRedisDao;
import com.github.peacockxt.infrastructure.cache.models.CacheUser;
import com.github.peacockxt.models.User;
import com.github.peacockxt.repository.implementation.UserRepository;
import com.github.peacockxt.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserLoginService {

    private static final int ACCESS_TOKEN_EXP = 900_000;
    private static final int REFRESH_TOKEN_EXP = 604_800_000;

    private final UserRepository userRepository;
    private final UserSessionRedisDao userSessionRedisDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.getUserByUserName(request.username());

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không đúng");
        }

        String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getUserName(), user.getSystemRole(), ACCESS_TOKEN_EXP);
        String refreshToken = jwtService.generateRefreshToken(user.getUserId(), REFRESH_TOKEN_EXP);

        CacheUser cacheUser = CacheUser.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .password(user.getPassword())
                .systemRole(user.getSystemRole())
                .config(user.getConfig())
                .status(user.getStatus())
                .updateAt(user.getUpdateAt())
                .createAt(user.getCreateAt())
                .createBy(user.getCreateBy())
                .updateBy(user.getUpdateBy())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .build();

        userSessionRedisDao.saveUserProfile(user.getUserId(), cacheUser);
        userSessionRedisDao.saveSessionToken(user.getUserId(), accessToken);
        userSessionRedisDao.saveRefreshToken(user.getUserId(), refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public TokenResponse refresh(String refreshToken) {
        String userIdStr = userSessionRedisDao.getUserIdByRefreshToken(refreshToken);

        if (userIdStr == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh Token không hợp lệ hoặc đã hết hạn. Yêu cầu login lại!");
        }

        Long userId = Long.parseLong(userIdStr);

        User user = userRepository.getUserByUserId(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User không tồn tại");
        }

        String newAccessToken = jwtService.generateAccessToken(user.getUserId(), user.getUserName(), user.getSystemRole(), ACCESS_TOKEN_EXP);
        userSessionRedisDao.saveSessionToken(userId, newAccessToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
