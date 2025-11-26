package com.example.lovequery.domain.log.entity;

import com.example.lovequery.common.BaseEntity;
import com.example.lovequery.domain.player.entity.Player;
import com.example.lovequery.domain.story.entity.Choice;
import com.example.lovequery.domain.story.entity.Episode;
import com.example.lovequery.domain.story.entity.Route;
import jakarta.persistence.*;
import lombok.Getter;


@Entity
@Table(
        name = "play_log",
        indexes = {
                @Index(name = "idx_play_log_player_route", columnList = "player_id, route_id"),
                @Index(name = "idx_play_log_session_created", columnList = "created_at, session_id")
        }
)
@Getter
public class PlayLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // log_id

    //논리적 세션키 playerid - routeid 조합
    @Column(name="session_id", nullable = false,length=50)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ep_id")
    private Episode episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id")
    private Choice choice;

    @Column(nullable = false)
    private Integer affectionBefore;

    @Column(nullable = false)
    private Integer affectionAfter;


    protected PlayLog() {}

    public PlayLog(
            String sessionId,
            Player player,
            Route route,
            Episode episode,
            Choice choice,
            Integer affectionBefore,
            Integer affectionAfter
    ) {
        this.sessionId = sessionId;
        this.player = player;
        this.route = route;
        this.episode = episode;
        this.choice = choice;
        this.affectionBefore = affectionBefore;
        this.affectionAfter = affectionAfter;
    }
}
