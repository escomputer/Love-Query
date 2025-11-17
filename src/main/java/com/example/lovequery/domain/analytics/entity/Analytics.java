package com.example.lovequery.domain.analytics.entity;


import com.example.lovequery.common.BaseEntity;
import com.example.lovequery.domain.story.entity.Choice;
import com.example.lovequery.domain.story.entity.Episode;
import jakarta.persistence.*;

@Entity
@Table(
        name = "analytics",
        indexes = {
                @Index(name = "idx_analytics_kind_updated", columnList = "kind, updatedAt")
        }
)
public class Analytics extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // proposal에서는 {kind, choice_id, ep_id} PK였지만, 실무는 단일PK가 편함

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AnalyticsKind kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id")
    private Choice choice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ep_id")
    private Episode episode;

    private Long pickCount;   // 선택 횟수
    private Long passCount;   // 성공 횟수
    private Long visitCount;  // 에피소드 방문 횟수
    private Long clearCount;  // 엔딩 도달 횟수

    protected Analytics() {}
}
