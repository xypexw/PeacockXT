package com.example.peacockxt.Controller.DTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    public  String userName;
    public  String password;
    public  String idempotencyKey;
}
