package com.example.lovequery.domain.story.dto.choice;

import com.example.lovequery.domain.story.entity.Choice;

public record ChoiceDto(
        Long id,
        String text,
        Long nextEpisodeIfPassId,
        Long nextEpisodeIfFailId,
        Integer affectionDelta,
        Integer threshold,
        Integer minRequiredAffection,
        boolean locked
) {
    public static ChoiceDto from(Choice c,int currentAffection) {
        Integer minAffection = c.getMinRequiredAffection();
        boolean locked =(minAffection!=null && currentAffection<minAffection);

        return new ChoiceDto(c.getId(),c.getText(),
        c.getNextEpisodeIfPass()!=null?c.getNextEpisodeIfPass().getId():null,
                c.getNextEpisodeIfFail()!=null?c.getNextEpisodeIfFail().getId():null,
                c.getAffectionDelta(),
                c.getThreshold(),
                minAffection,
                locked);
    }
}
