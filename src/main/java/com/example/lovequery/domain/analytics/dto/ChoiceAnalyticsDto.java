package com.example.lovequery.domain.analytics.dto;

public record ChoiceAnalyticsDto(
        Long choiceId,
        String choiceText,
        Long epId,
        Integer threshold,
        long pickCount,
        long passCount,
        long failCount,
        double passRate
) {
}
