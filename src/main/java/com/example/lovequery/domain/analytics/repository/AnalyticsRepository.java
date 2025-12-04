package com.example.lovequery.domain.analytics.repository;

import com.example.lovequery.domain.analytics.entity.Analytics;
import com.example.lovequery.domain.analytics.entity.AnalyticsKind;
import com.example.lovequery.domain.story.entity.Choice;
import com.example.lovequery.domain.story.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {

    Optional<Analytics> findByKindAndEpisode(AnalyticsKind kind, Episode episode);

    Optional<Analytics> findByKindAndChoice(AnalyticsKind kind, Choice choice);

    //route 기준 ep 통계
    @Query("""
            select a from Analytics a join a.episode e where a.kind =:kind and e.route.id =:routeId
            """)
    List<Analytics> findEpisodeStatsByRoute(@Param("kind") AnalyticsKind kind, @Param("routeId") Long routeId);

    //route 기준 choice 통계
    @Query("""
            select a from Analytics a join a.choice c join c.episode e where a.kind =:kind and e.route.id =:routeId
            """)
    List<Analytics> findChoiceStatsByRoute(@Param("kind") AnalyticsKind kind, @Param("routeId") Long routeId);

    void deleteByChoiceId(Long choiceId);
    void deleteByEpisodeId(Long episodeId);

}
