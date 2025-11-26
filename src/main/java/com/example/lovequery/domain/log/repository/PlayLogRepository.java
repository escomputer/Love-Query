package com.example.lovequery.domain.log.repository;

import com.example.lovequery.domain.log.entity.PlayLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayLogRepository extends JpaRepository<PlayLog,Integer> {

    List<PlayLog> findByRouteIdAndPlayerIdAndSessionIdOrderByCreatedAtAsc(Long routeId,Long playerId,String sessionId);

    Optional<PlayLog> findTopByPlayerIdOrderByCreatedAtDesc(Long playerId);
}
