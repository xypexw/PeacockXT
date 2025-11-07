package com.example.peacockxt.Service.SystemModule;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.UserModule.User;
import com.example.peacockxt.Repository.Implimentation.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public LoginService(PasswordEncoder passwordEncoder, UserRepository userRepository, RedisTemplate<String, User> userRedisTemplate) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }


    public boolean login(String username, String password){
        String hashedPassword = passwordEncoder.encode(password);
        User user = userRepository.getUserByUserName(username);
        if(user==null){
            return false;
        }
        if(user.getPassword().equals(hashedPassword)){
            return true;
        }
        return false;
    }

    public User getUserByUserName(String userName){
        return userRepository.getUserByUserName(userName);
    }

}
