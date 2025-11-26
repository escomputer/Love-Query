package com.example.lovequery.domain.story.dto;

import com.example.lovequery.common.EndingType;
import com.example.lovequery.domain.story.dto.choice.ChoiceDto;

import java.util.List;

public record GameStateDto(
        Long epId,
        String epText,
        List<ChoiceDto> choices,
        int affection,
        boolean isEnding,
        String endingLabel,
        EndingType branch // BAD NORMAL TRUE
) {


}
