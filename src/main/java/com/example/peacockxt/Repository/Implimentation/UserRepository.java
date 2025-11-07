package com.example.peacockxt.Repository.Implimentation;
import java.util.List;
import com.example.peacockxt.Models.GroupModule.Team;
import com.example.peacockxt.Models.UserModule.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User getUserByUserName(String userName);

    User getUserByUserId(String userId);
}
