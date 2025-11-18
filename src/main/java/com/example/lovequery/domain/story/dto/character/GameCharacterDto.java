package com.example.lovequery.domain.story.dto.character;

import com.example.lovequery.domain.story.entity.GameCharacter;

public record GameCharacterDto(
        Long id,
        String name,
        String personality
) {

    public static GameCharacterDto from(GameCharacter c) {
        return new GameCharacterDto(c.getId(),c.getName(),c.getPersonality());
    }
}
