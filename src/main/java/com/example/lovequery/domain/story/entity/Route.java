package com.example.lovequery.domain.story.entity;

import com.example.lovequery.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(
        name="routes",
        indexes = {
                @Index(name = "idx_routes_char_id", columnList = "character_id")
        }
)
@Getter
public class Route extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // route_id

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id")
    private GameCharacter character;

    @Column(nullable = false, length = 100)
    private String title;

    // 나중에 bad ending episode FK 연결할 수 있음
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bad_ending_ep_id")
    private Episode badEndingEpisode;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "normal_ending_ep_id")
    private Episode normalEndingEpisode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "true_ending_ep_id")
    private Episode trueEndingEpisode;

    @Column(nullable = false)
    private Integer minAffectionRequired;  // 엔딩 진입 최소 호감도

    @Column(nullable = false)
    private Integer trueEndingThreshold;




    @Column(length = 255)
    private String warningText;

    protected Route() {}

    public Route(GameCharacter character,
                 String title,
                 Episode badEndingEpisode,
                 Episode normalEndingEpisode,
                 Episode trueEndingEpisode,
                 Integer minAffectionRequired,
                 Integer trueEndingThreshold,
                 String warningText) {

        this.character = character;
        this.title = title;
        this.badEndingEpisode = badEndingEpisode;
        this.normalEndingEpisode = normalEndingEpisode;
        this.trueEndingEpisode = trueEndingEpisode;
        this.minAffectionRequired = minAffectionRequired;
        this.trueEndingThreshold = trueEndingThreshold;
        this.warningText = warningText;
    }
}
