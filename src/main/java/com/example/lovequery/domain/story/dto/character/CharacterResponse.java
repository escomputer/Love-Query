package com.example.lovequery.domain.story.dto.character;

public record CharacterResponse(
        Long id,
        String name,
        String gender,
        String personality,
        Integer affinityCap,
        Integer popularityScore
) {
}
