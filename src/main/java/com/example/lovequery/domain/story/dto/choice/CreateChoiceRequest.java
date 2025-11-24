package com.example.lovequery.domain.story.dto.choice;

public record CreateChoiceRequest(
        String text,
        Long nextEpIfFailId,
        Long nextEpIfPassId,
        Integer affectionDelta,
        Integer threshold
) {
}
