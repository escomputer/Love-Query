package com.example.lovequery.domain.story.dto.character;

public record UpdateCharacterRequest(
        String name,
        String gender,
        String personality,
        Integer affinityCap
) {
}
