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

    @Column(name = "start_episode_id")
    private Long startEpisodeId;



    @Column(nullable = false)
    private Integer minAffectionRequired;  // 엔딩 진입 최소 호감도

    @Column(nullable = false)
    private Integer trueEndingThreshold;




    @Column(length = 255)
    private String warningText;

    protected Route() {}

    public Route(GameCharacter character,
                 String title,
                 Integer minAffectionRequired,
                 Integer trueEndingThreshold,
                 String warningText) {

        this.character = character;
        this.title = title;
        this.minAffectionRequired = minAffectionRequired;
        this.trueEndingThreshold = trueEndingThreshold;
        this.warningText = warningText;
    }

    public void update(String title, Integer minAffectionRequired, Long startEpisodeId,Integer trueEndingThreshold, String warningText) {
        this.title = title;
        this.minAffectionRequired = minAffectionRequired;
        this.startEpisodeId = startEpisodeId;
        this.trueEndingThreshold = trueEndingThreshold;
        this.warningText = warningText;
    }
}
