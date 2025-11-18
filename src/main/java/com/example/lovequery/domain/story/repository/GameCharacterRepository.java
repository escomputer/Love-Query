package com.example.lovequery.domain.story.repository;

import com.example.lovequery.domain.story.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {
}
