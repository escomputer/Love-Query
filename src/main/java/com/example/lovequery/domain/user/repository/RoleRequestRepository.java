package com.example.lovequery.domain.user.repository;

import com.example.lovequery.domain.user.entity.RoleRequest;
import com.example.lovequery.domain.user.entity.RoleRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRequestRepository extends JpaRepository<RoleRequest, Long> {
    List<RoleRequest> findByStatus(RoleRequestStatus status);
}
