package com.example.lovequery.domain.story.repository;

import com.example.lovequery.common.EndingType;
import com.example.lovequery.domain.story.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EpisodeRepository extends JpaRepository<Episode,Long> {

    List<Episode> findByRouteIdOrderByIdAsc(Long routeId);

    Optional<Episode> findFirstByRouteIdOrderByIdAsc(Long routeId);


    Optional<Episode> findFirstByRouteIdAndEndingType(Long routeId, EndingType endingType);
}
