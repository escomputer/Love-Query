package com.example.lovequery.domain.story.dto.ep;

public record CreateEpisodeRequest(
        Long routeId,
        String text
) {
}
