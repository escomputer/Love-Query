package com.example.lovequery.domain.story.dto.route;

public record UpdateRouteRequest(
        Long characterId,
        String title,
        Integer minAffectionRequired,
        Integer trueEndingThreshold,
        String warningText,
        Long startEpisodeId
) {
}
