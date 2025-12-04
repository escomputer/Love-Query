package com.example.lovequery.domain.story.dto.ep;

import com.example.lovequery.common.EndingType;

public record UpdateEpisodeRequest(
        Long routeId,
        String title,
        String text,
        Boolean isEnding,
        EndingType endingType
) {
}
