package com.example.lovequery.domain.story.dto.choice;

public record ChoiceResponse(
        Long id,
        Long epId,
        String text,
        Long nextEpIfFailId,
        Long nextEpIfSuccId,

        Integer affectionDelta,
        Integer threshold
) {
}
