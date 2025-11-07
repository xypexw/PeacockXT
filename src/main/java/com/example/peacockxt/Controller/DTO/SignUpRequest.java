package com.example.peacockxt.Controller.DTO;
import lombok.Data;

@Data
public class SignUpRequest {
    public String email;
    public String password;
    public String firstName;
    public String lastName;
    public String bio;
    public String idempotencyKey;
}
