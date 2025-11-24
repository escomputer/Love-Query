package com.example.lovequery.domain.story.dto.route;

public record CreateRouteRequest(
        Long characterId,
        String title,
        Integer minAffectionRequired,
        Integer trueEndingThreshold,
        Long badEndingEpId,
        Long normalEndingEpId,
        Long trueEndingEpId,
        String warningText
) {
}
