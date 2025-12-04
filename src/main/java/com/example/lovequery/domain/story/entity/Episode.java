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

    @Column(length = 100)
    private String title;

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

    public Episode(Route route, String title, String text, Boolean isEnding, EndingType endingType) {
        this.route = route;
        this.title = (title != null && !title.isBlank()) ? title : null;
        this.text = text;
        this.isEnding = (isEnding != null) ? isEnding : false;
        this.endingType = endingType;
    }


    public void update(String title, String text, Boolean isEnding, EndingType endingType) {
        this.title = (title != null && !title.isBlank()) ? title : null;
        this.text = text;
        this.isEnding = isEnding != null ? isEnding : false;
        this.endingType = endingType;
    }

}
