package com.example.lovequery.domain.story.dto.ep;

import com.example.lovequery.common.EndingType;

public record CreateEpisodeRequest(
        Long routeId,
        String text,
        Boolean isEnding,
        EndingType endingType
) {
}
