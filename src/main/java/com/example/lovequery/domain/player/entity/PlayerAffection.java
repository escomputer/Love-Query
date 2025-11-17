package com.example.lovequery.domain.player.entity;

import com.example.lovequery.domain.story.entity.GameCharacter;
import jakarta.persistence.*;

@Entity
@Table(
        name = "player_affection",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_player_character",
                        columnNames = {"player_id", "character_id"}
                )
        }
)
public class PlayerAffection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id")
    private GameCharacter character;

    @Column(nullable = false)
    private Integer score; // 0 ~ affinityCap (상한은 비즈니스로직에서 체크)

    protected PlayerAffection() {
    }

    public PlayerAffection(Player player, GameCharacter character, Integer score) {
        this.player = player;
        this.character = character;
        this.score = score;
    }

    // getter 정도는 필요에 따라
    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public GameCharacter getCharacter() {
        return character;
    }

    public Integer getScore() {
        return score;
    }

    public void increaseScore(int delta) {
        this.score += delta;
    }

    public void updateScore(int newScore) {
        this.score = newScore;
    }


}