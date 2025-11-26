package com.example.lovequery.domain.analytics.dto;

public record RouteAnalyticsDto(
        Long routeId,
        String routeTitle,
        String charName,
        long totalVisits,
        long totalClears,
        long trueEndingClears,
        long normalEndingClears,
        long badEndingClears
) {
}
