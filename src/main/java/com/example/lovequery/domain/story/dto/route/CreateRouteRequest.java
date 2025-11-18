package com.example.lovequery.domain.story.dto.route;

public record CreateRouteRequest(
        Long charId,
        String title,
        Integer minAffectionRequired,
        String warningText
) {
}
