package com.example.lovequery.domain.user.dto;

import com.example.lovequery.domain.user.entity.Role;


public record SignUpRequest(
        String email,
        String password,
        Role role,
        String name){

}
