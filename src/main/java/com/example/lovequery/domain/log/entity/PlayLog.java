package com.example.lovequery.domain.log.entity;

import com.example.lovequery.common.BaseEntity;
import com.example.lovequery.domain.player.entity.Player;
import com.example.lovequery.domain.story.entity.Choice;
import com.example.lovequery.domain.story.entity.Episode;
import jakarta.persistence.*;


@Entity
@Table(
        name = "play_log",
        indexes = {
                @Index(name = "idx_play_log_player_created", columnList = "player_id, createdAt"),
                @Index(name = "idx_play_log_choice_id", columnList = "choice_id")
        }
)
public class PlayLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // log_id

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ep_id")
    private Episode episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id")
    private Choice choice;

    @Column(nullable = false)
    private Boolean isPass;   // 성공/실패

    protected PlayLog() {}

    public PlayLog(Player player, Episode episode, Choice choice, boolean isPass) {
        this.player = player;
        this.episode = episode;
        this.choice = choice;
        this.isPass = isPass;
    }
}
