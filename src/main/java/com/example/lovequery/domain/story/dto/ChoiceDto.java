package com.example.lovequery.domain.story.dto;

import com.example.lovequery.domain.story.entity.Choice;

public record ChoiceDto(
        Long id,
        String text
) {
    public static ChoiceDto from(Choice c) {
        return new ChoiceDto(c.getId(),c.getText());
    }
}
