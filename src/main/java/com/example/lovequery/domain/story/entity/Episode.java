package com.example.lovequery.domain.story.entity;

import com.example.lovequery.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(
        name = "episodes",
        indexes = {
                @Index(name = "idx_episodes_route_id", columnList = "route_id")
        }
)
@Getter
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

    public Episode(Route route, String text,Boolean isEnding, String endingLabel) {
        this.route = route;
        this.text = text;
        this.isEnding=(isEnding!=null)?isEnding:false;
        this.endingLabel=endingLabel;
    }

    public void changeBackground(String bgAsset) {
        this.bgAsset = bgAsset;
    }

    public void changeMusic(String musicAsset) {
        this.musicAsset = musicAsset;
    }

    public void markAsEnding(String endingLabel) {
        this.isEnding = true;
        this.endingLabel = endingLabel;
    }

}
