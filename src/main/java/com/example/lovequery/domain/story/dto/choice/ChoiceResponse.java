package com.example.lovequery.domain.story.dto.choice;

public record ChoiceResponse(
        Long id,
        Long epId,
        String text,
        Long nextEpIfFailId,
        Long nextEpIfPassId,

        Integer affectionDelta,
        Integer threshold,
        Integer minAffection
) {
}
