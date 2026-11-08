package com.example.peacockxt.Repository.Imlpementation;

import com.example.peacockxt.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    User getUserByUserId(Long userId);


    User getUserByUserName(String userName);
}
