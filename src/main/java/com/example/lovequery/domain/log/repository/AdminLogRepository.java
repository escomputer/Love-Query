package com.example.lovequery.domain.log.repository;

import com.example.lovequery.domain.log.entity.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    // 최신순 조회
    List<AdminLog> findAllByOrderByCreatedAtDesc();
}