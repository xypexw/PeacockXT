package com.example.peacockxt.Service.SystemModule;

import com.example.peacockxt.Models.UserModule.User;
import com.example.peacockxt.Repository.Implimentation.UserRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SignUpService {

    private final RedisTemplate<String, User> redisTemplate;
    private final UserRepository userRepository;
    private final String prefixKey = "user::";
    private final int timeToLive = 10;
    private final PasswordEncoder passwordEncoder ;

    SignUpService(RedisTemplate<String, User> redisTemplate, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Transactional
    public void signUp(String email, String password, String firstName, String lastName
            , String bio ) {
        final String renderKey = UuidCreator.getTimeOrdered().toString();
        User user = User.builder().userId(renderKey)
                .email(email).password(password)
                .firstName(firstName).lastName(lastName).bio(bio).build();
    }


}
