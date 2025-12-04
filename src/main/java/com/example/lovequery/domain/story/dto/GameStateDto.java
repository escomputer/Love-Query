package com.example.lovequery.domain.story.dto;

import com.example.lovequery.common.EndingType;
import com.example.lovequery.domain.story.dto.choice.ChoiceDto;

import java.util.List;

public record GameStateDto(
        Long epId,
        String epTitle,
        String epText,
        List<ChoiceDto> choices,
        int affection,
        boolean isEnding,
        EndingType branch // BAD NORMAL TRUE
) {


}
