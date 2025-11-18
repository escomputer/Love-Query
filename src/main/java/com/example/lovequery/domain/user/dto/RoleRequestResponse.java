package com.example.lovequery.domain.user.dto;

import com.example.lovequery.domain.user.entity.Role;
import com.example.lovequery.domain.user.entity.RoleRequest;
import com.example.lovequery.domain.user.entity.RoleRequestStatus;

public record RoleRequestResponse(
        Long id,
        Long userId,
        String email,
        Role requestedRole,
        RoleRequestStatus requestStatus
) {
    public static RoleRequestResponse from(RoleRequest roleRequest) {
        return new RoleRequestResponse(
                roleRequest.getId(),
                roleRequest.getUser().getId(),
                roleRequest.getUser().getEmail(),
                roleRequest.getRequestRole(),
                roleRequest.getStatus()
        );
    }
}
