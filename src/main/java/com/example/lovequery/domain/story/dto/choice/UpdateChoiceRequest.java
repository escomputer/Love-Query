package com.example.lovequery.domain.story.dto.choice;

public record UpdateChoiceRequest(
        String text,
        Long nextEpIfFailId,
        Long nextEpIfPassId,
        Integer affectionDelta,
        Integer threshold,
        Integer minRequiredAffection
) {
}
