package com.example.lovequery.domain.story.dto.route;

public record CreateRouteRequest(
        Long characterId,
        String title,
        Integer minAffectionRequired,
        Integer trueEndingThreshold,
        String warningText
) {
}
