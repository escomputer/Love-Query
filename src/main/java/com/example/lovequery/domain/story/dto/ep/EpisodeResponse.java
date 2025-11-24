package com.example.lovequery.domain.story.dto.ep;

public record EpisodeResponse(
        Long id,
        Long routeId,
        String text,
        Boolean isEnding,
        String endingLabel
) {
}
