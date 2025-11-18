package com.example.lovequery.domain.story.entity;

import com.example.lovequery.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(
        name = "choices",
        indexes = {
                @Index(name = "idx_choices_ep_id", columnList = "ep_id"),
                @Index(name = "idx_choices_next_pass", columnList = "next_ep_if_pass_id"),
                @Index(name = "idx_choices_next_fail", columnList = "next_ep_if_fail_id")
        }
)
@Getter
public class Choice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // choice_id

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ep_id")
    private Episode episode;

    @Lob
    @Column(nullable = false)
    private String text;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_ep_if_pass_id")
    private Episode nextEpisodeIfPass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_ep_if_fail_id")
    private Episode nextEpisodeIfFail;

    @Column(nullable = false)
    private Integer affectionDelta = 0;   // 호감도 변화

    @Column(nullable = false)
    private Integer threshold = 0;        // 통과 기준 호감도 등

    @Column(length = 10)
    private String qualityTag;            // "BAD", "OK", "GOOD"

    @Column(length = 255)
    private String tipText;               // 최종 리포트용 힌트

    protected Choice() {}
}
