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

    @Column(columnDefinition ="text",nullable = false)
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

    @Column(name="min_required_affection")
    private Integer minRequiredAffection;

    @Column(length = 255)
    private String tipText;               // 최종 리포트용 힌트

    protected Choice() {}

    public Choice(Episode episode, String text, Episode nextFail, Episode nextPass , Integer affectionDelta, Integer threshold,Integer minRequiredAffection) {
        this.episode = episode;
        this.text = text;
        this.nextEpisodeIfPass = nextPass;
        this.nextEpisodeIfFail = nextFail;
        this.affectionDelta = affectionDelta;
        this.threshold = threshold;
        this.minRequiredAffection = minRequiredAffection;

    }

    public void updateTipText(String tipText) {
        this.tipText = tipText;
    }

    public void update(String text, Episode nextPass, Episode nextFail, Integer threshold, Integer affectionDelta, Integer minRequiredAffection) {
        this.text = text;
        this.nextEpisodeIfPass = nextPass;
        this.nextEpisodeIfFail = nextFail;
        this.threshold = threshold;
        this.affectionDelta = affectionDelta;
        this.minRequiredAffection = minRequiredAffection;
    }
}
