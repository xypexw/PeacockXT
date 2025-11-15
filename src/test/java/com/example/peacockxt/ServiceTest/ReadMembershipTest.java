package com.example.peacockxt.ServiceTest;
import com.example.peacockxt.Repository.Implimentation.MembershipRepository;
import com.example.peacockxt.Repository.Implimentation.UserRepository;
import com.example.peacockxt.Service.MembershipService.HelperMBSService;
import com.example.peacockxt.Service.MembershipService.ReadMembershipService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
@ExtendWith(MockitoExtension.class)
public class ReadMembershipTest {
    private StringRedisTemplate redisTemplate;
    private UserRepository userRepository;
    private MembershipRepository membershipRepository;
    private HelperMBSService helperMBSService;
    private ReadMembershipService readMembershipService;
    private ListOperations<String, String> listOps;

}


