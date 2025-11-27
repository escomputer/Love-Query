package com.example.lovequery.domain.log.entity;

import com.example.lovequery.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_logs", indexes = @Index(name = "idx_admin_logs_created", columnList = "createdAt"))
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AdminLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adminId; // 작업을 수행한 관리자/작가 id

    @Column(nullable = false, length = 20)
    private String action; // "CREATE", "UPDATE", "DELETE"

    @Column(nullable = false, length = 50)
    private String targetType; // "CHARACTER", "ROUTE", "EPISODE", "CHOICE"

    @Column(nullable = false)
    private Long targetId; // 변경된 데이터의 ID

    @Column(length = 500)
    private String description; // 상세 내용 (옵션)

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public AdminLog(Long adminId, String action, String targetType, Long targetId, String description) {
        this.adminId = adminId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.description = description;
    }
}