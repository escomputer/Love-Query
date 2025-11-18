package com.example.lovequery.domain.story.dto.character;

public record CreateCharacterRequest(
        String name,
        String gender,
        String personality,
        Integer affinityCap
) {
}
