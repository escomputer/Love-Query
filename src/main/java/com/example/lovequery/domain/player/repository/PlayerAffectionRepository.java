package com.example.lovequery.domain.player.repository;

import com.example.lovequery.domain.player.entity.PlayerAffection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerAffectionRepository extends JpaRepository<PlayerAffection,Long> {

    Optional<PlayerAffection> findByPlayerIdAndCharacterId(Long playerId,Long characterId);
    List<PlayerAffection> findByPlayerId(Long playerId);
}
