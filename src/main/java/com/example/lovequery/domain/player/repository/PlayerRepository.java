package com.example.lovequery.domain.player.repository;

import com.example.lovequery.domain.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player,Long> {

    Optional<Player> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE Player p SET p.currentEpisode = NULL WHERE p.currentEpisode.id = :episodeId")
    void detachEpisodeFromPlayers(@Param("episodeId") Long episodeId);
}
