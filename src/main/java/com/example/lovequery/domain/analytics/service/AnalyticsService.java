package com.example.lovequery.domain.analytics.service;

import com.example.lovequery.domain.analytics.entity.Analytics;
import com.example.lovequery.domain.analytics.entity.AnalyticsKind;
import com.example.lovequery.domain.analytics.repository.AnalyticsRepository;
import com.example.lovequery.domain.story.entity.Choice;
import com.example.lovequery.domain.story.entity.Episode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    /**
     * 에피소드 방문 기록
     */
    @Transactional
    public void recordEpVisit(Episode episode) {
        Analytics analytics = analyticsRepository.findByKindAndEpisode(AnalyticsKind.EPISODE,episode)
                .orElseGet(()->analyticsRepository.save(Analytics.createForEpisode(episode)));

        analytics.increaseVisit();

    }

    /**
     * 선택, pass/fail
     */
    @Transactional
    public void recordChoice(Choice choice,boolean pass) {
        Analytics analytics = analyticsRepository.findByKindAndChoice(AnalyticsKind.CHOICE,choice)
                .orElseGet(()->analyticsRepository.save(Analytics.createForChoice(choice)));

        analytics.increasePick(pass);
    }

    /**
     * ending
     */
    @Transactional
    public void recordEndingClear(Episode endingEp) {
        Analytics analytics = analyticsRepository
                .findByKindAndEpisode(AnalyticsKind.EPISODE,endingEp)
                .orElseGet(()->analyticsRepository.save(Analytics.createForEpisode(endingEp)));

        analytics.increaseClear();
    }
}
