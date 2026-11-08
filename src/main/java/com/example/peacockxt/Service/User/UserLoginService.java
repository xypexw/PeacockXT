package com.example.peacockxt.Service.User;

import com.example.peacockxt.DTO.Request.LoginRequest;
import com.example.peacockxt.DTO.Respone.TokenResponse;
import com.example.peacockxt.Infrastructure.Cache.UserSessionRedisDao;
import com.example.peacockxt.Models.User;
import com.example.peacockxt.Repository.Imlpementation.UserRepository;
import com.example.peacockxt.Security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserLoginService {

    private static final int ACCESS_TOKEN_EXP = 900_000;       // 15 phút
    private static final int REFRESH_TOKEN_EXP = 604_800_000;  // 7 ngày

    private final UserRepository userRepository;
    private final UserSessionRedisDao userSessionRedisDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // 1. LUỒNG ĐĂNG NHẬP
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.getUserByUserName(request.username());

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không đúng");
        }

        // Tạo 2 token
        String accessToken = jwtService.generateAccessToken(user.getUserId(), user.getUserName(), user.getSystemRole(), ACCESS_TOKEN_EXP);
        String refreshToken = jwtService.generateRefreshToken(user.getUserId(), REFRESH_TOKEN_EXP);

        // Lưu vào Redis để APISIX có thể verify
        userSessionRedisDao.saveUserProfile(user.getUserId(), user);
        userSessionRedisDao.saveSessionToken(user.getUserId(), accessToken);
        userSessionRedisDao.saveRefreshToken(user.getUserId(), refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // 2. LUỒNG REFRESH TOKEN (Cấp lại Access Token mới)
    public TokenResponse refresh(String refreshToken) {
        // Kiểm tra xem Refresh Token có tồn tại trong Redis không (đã bị block hoặc hết hạn chưa)
        String userIdStr = userSessionRedisDao.getUserIdByRefreshToken(refreshToken);

        if (userIdStr == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh Token không hợp lệ hoặc đã hết hạn. Yêu cầu login lại!");
        }

        Long userId = Long.parseLong(userIdStr);

        // Móc user từ Database (hoặc Redis Profile) để lấy lại Role/Username
        User user = userRepository.getUserByUserId(userId); // Giả sử bạn có hàm này
        if(user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User không tồn tại");
        }

        // Tạo DUY NHẤT Access Token mới (Refresh token giữ nguyên để user không bị log out)
        String newAccessToken = jwtService.generateAccessToken(user.getUserId(), user.getUserName(), user.getSystemRole(), ACCESS_TOKEN_EXP);

        // Cập nhật Access Token mới vào Redis cho APISIX check
        userSessionRedisDao.saveSessionToken(userId, newAccessToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Trả về lại cái cũ
                .build();
    }
}