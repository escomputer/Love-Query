package com.example.lovequery.domain.analytics.dto;

public record EpisodeAnalyticsDto(
        Long epId,
        String epText,
        long visitcout,
        long clearCout,
        double clearRate
) {
}
