package com.example.lovequery.domain.user.dto;

import com.example.lovequery.domain.user.entity.Role;

public record LoginResponse(
        Long userId,
        String name,
        Role role
) {
}
