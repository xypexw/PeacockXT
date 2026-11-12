package com.github.peacockxt.repository.implementation;

import com.github.peacockxt.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    User getUserByUserId(Long userId);


    User getUserByUserName(String userName);
}
