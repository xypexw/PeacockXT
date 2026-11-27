package com.github.peacockxt.infrastructure.cache.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheUser {
    private Long userId;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String systemRole;
    private String config;
    private String status;
    private LocalDateTime updateAt;
    private LocalDateTime createAt;
    private Long createBy;
    private Long updateBy;
    private String avatarUrl;
    private String bio;
}
