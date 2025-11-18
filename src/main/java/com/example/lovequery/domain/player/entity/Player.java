package com.example.lovequery.domain.player.entity;


import com.example.lovequery.common.BaseEntity;
import com.example.lovequery.domain.story.entity.Episode;
import com.example.lovequery.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name ="players",
        indexes = {
                @Index(name = "idx_players_user_id", columnList = "user_id"),
                @Index(name = "idx_players_current_ep_id", columnList = "current_ep_id")
        }
)
@Getter
public class Player extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //optional은 필수관계 userId 절대 null 불가
    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name="user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="current_ep_id")
    private Episode currentEpisode;

    protected Player() {

    }

    public Player(User user) {
        this.user = user;
    }

    public void changeCurrentEpisode(Episode episode) {
        this.currentEpisode = episode;
    }



}
