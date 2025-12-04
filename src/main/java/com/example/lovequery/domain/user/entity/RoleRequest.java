package com.example.lovequery.domain.user.entity;

import com.example.lovequery.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name="role_requests")
public class RoleRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role requestRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoleRequestStatus status ;

    protected RoleRequest() {

    }

    public RoleRequest(User user, Role requestRole) {
        this.user = user;
        this.requestRole = requestRole;
        this.status = RoleRequestStatus.PENDING;
    }

    public void approve() {
        this.status = RoleRequestStatus.APPROVED;
    }
    public void reject() {
        this.status = RoleRequestStatus.REJECTED;
    }
}
