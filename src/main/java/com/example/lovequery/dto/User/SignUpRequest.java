package com.example.lovequery.dto.User;

import com.example.lovequery.entity.Role;
import lombok.Getter;

@Getter
public class SignUpRequest {

    private String email;
    private String password;
    private Role role;
    private String name;
}
