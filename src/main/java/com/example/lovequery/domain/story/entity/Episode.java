package com.example.lovequery.domain.story.entity;

import com.example.lovequery.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "episodes",
        indexes = {
                @Index(name = "idx_episodes_route_id", columnList = "route_id")
        }
)
public class Episode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name="route_id")
    private Route route;

    @Lob
    @Column(nullable = false)
    private String text;

    @Column(length = 255)
    private String bgAsset;

    @Column(length = 255)
    private String musicAsset;

    @Column(nullable = false)
    private Boolean isEnding = false;

    @Column(length = 100)
    private String endingLabel;

    protected Episode() {}

}
