package com.example.lovequery.domain.story.dto.route;

public record RouteResponse(
        Long id,
        Long charId,
        String charName,
        String title,
        Integer minAffectionRequired,
        Integer trueEndingThreshold,
        String warningText
) {
}
