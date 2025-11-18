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

    @Column(nullable = false)
    private Integer minAffectionRequired;  // 엔딩 진입 최소 호감도

    @Column(length = 255)
    private String warningText;

    protected Route() {}

    public Route(GameCharacter character, String title, Integer minAffectionRequired, String warningText) {
        this.character = character;
        this.title = title;
        this.minAffectionRequired = minAffectionRequired;
        this.warningText = warningText;
    }
}
