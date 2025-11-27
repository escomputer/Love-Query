package com.example.lovequery.domain.story.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "characters",
        indexes = {
                @Index(name = "idx_characters_popularity", columnList = "popularityScore")
        })
@Getter
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 10)
    private String gender;   // "M", "F", "OTHER" 정도. ENUM으로 빼도 됨

    @Column(length = 100)
    private String personality;

    @Column(nullable = false)
    private Integer affinityCap;      // 호감도 상한

    @Column(nullable = false)
    private Integer popularityScore = 0;  // 인기 점수

    protected GameCharacter() {}

    public GameCharacter(String name, String gender, String personality, Integer affinityCap){
        this.name = name;
        this.gender = gender;
        this.personality = personality;
        this.affinityCap = affinityCap;
    }

    public void update(String name, String gender, String personality, Integer affinityCap) {
        this.name = name;
        this.gender = gender;
        this.personality = personality;
        this.affinityCap = affinityCap;
    }

    public void increasePopularity() {
        if (this.popularityScore == null) {
            this.popularityScore = 0;
        }
        this.popularityScore++;
    }

}
