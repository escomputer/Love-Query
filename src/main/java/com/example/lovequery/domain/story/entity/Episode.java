package com.example.lovequery.domain.story.entity;

import com.example.lovequery.common.BaseEntity;
import com.example.lovequery.common.EndingType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(
        name = "episodes",
        indexes = {
                @Index(name = "idx_episodes_route_id", columnList = "route_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_route_ending_type",
                        columnNames = {"route_id", "ending_type"}
                )
        }
)
@Getter
public class Episode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id")
    private Route route;

    @Lob
    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Boolean isEnding = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EndingType endingType;

    protected Episode() {
    }

    public Episode(Route route, String text, Boolean isEnding, EndingType endingType) {
        this.route = route;
        this.text = text;
        this.isEnding = (isEnding != null) ? isEnding : false;
        this.endingType = endingType;
    }


    public void update(String text, Boolean isEnding, EndingType endingType) {
        this.text = text;
        this.isEnding = isEnding != null ? isEnding : false;
        this.endingType = endingType;
    }

}
