package com.example.lovequery.domain.analytics.entity;


import com.example.lovequery.common.BaseEntity;
import com.example.lovequery.domain.story.entity.Choice;
import com.example.lovequery.domain.story.entity.Episode;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(
        name = "analytics",
        indexes = {
                @Index(name = "idx_analytics_kind_updated", columnList = "kind, updatedAt")
        }
)
@Getter
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

    private Analytics(AnalyticsKind kind, Choice choice, Episode episode) {
        this.kind = kind;
        this.choice = choice;
        this.episode = episode;
        this.pickCount = 0L;
        this.passCount = 0L;
        this.visitCount = 0L;
        this.clearCount = 0L;
    }

    // 에피소드용
    public static Analytics createForEpisode(Episode episode) {
        return new Analytics(AnalyticsKind.EPISODE, null, episode);
    }

    // 선택지용
    public static Analytics createForChoice(Choice choice) {
        return new Analytics(AnalyticsKind.CHOICE, choice, null);
    }

    public void increaseVisit() {
        if (this.visitCount == null) this.visitCount = 0L;
        this.visitCount++;
    }

    public void increaseClear() {
        if (this.clearCount == null) this.clearCount = 0L;
        this.clearCount++;
    }

    public void increasePick(boolean pass) {
        if (this.pickCount == null) this.pickCount = 0L;
        this.pickCount++;
        if (pass) {
            if (this.passCount == null) this.passCount = 0L;
            this.passCount++;
        }
    }
}
