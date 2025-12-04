package com.example.lovequery.domain.log.dto;

public record PlayStepDto(
        Long epId,
        String epText,
        Long choiceId,
        String choiceText,
        int affectionBefore,
        int affectionAfter
) {
}
