package com.example.lovequery.domain.story.repository;

import com.example.lovequery.domain.story.entity.Choice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChoiceRepository extends JpaRepository<Choice,Long> {
    List<Choice> findByEpisodeId(Long epId);
}
