package com.example.lovequery.domain.story.repository;

import com.example.lovequery.domain.story.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route,Long> {

    List<Route> findByCharacterId(Long characterId);
}
