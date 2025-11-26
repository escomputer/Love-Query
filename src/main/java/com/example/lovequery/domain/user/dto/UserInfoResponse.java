package com.example.lovequery.domain.user.dto;

import com.example.lovequery.domain.user.entity.Role;

public record UserInfoResponse(
        Long id,
        String email,
        String name,
        Role role
) {
}
