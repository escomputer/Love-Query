package com.example.lovequery.domain.analytics.service;

import com.example.lovequery.common.EndingType;
import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.analytics.dto.ChoiceAnalyticsDto;
import com.example.lovequery.domain.analytics.dto.EpisodeAnalyticsDto;
import com.example.lovequery.domain.analytics.dto.RouteAnalyticsDto;
import com.example.lovequery.domain.analytics.entity.Analytics;
import com.example.lovequery.domain.analytics.entity.AnalyticsKind;
import com.example.lovequery.domain.analytics.repository.AnalyticsRepository;
import com.example.lovequery.domain.story.dto.route.RouteResponse;
import com.example.lovequery.domain.story.entity.Episode;
import com.example.lovequery.domain.story.entity.Route;
import com.example.lovequery.domain.story.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportForBalanceService {

    private final AnalyticsRepository analyticsRepository;
    private final RouteRepository routeRepository;

    /**
     * 특정 루트의 에피소드별 방문/클리어 통계
     */
    @Transactional(readOnly = true)
    public List<EpisodeAnalyticsDto> getEpisodeStatistics(Long routeId) {
        List<Analytics> list = analyticsRepository.
                findEpisodeStatsByRoute(AnalyticsKind.EPISODE,routeId);

        return list.stream()
                .map(a->{
                    Episode ep = a.getEpisode();
                    long visit = a.getVisitCount()!=null?a.getVisitCount():0L;
                    long clear = a.getClearCount()!=null?a.getClearCount():0L;
                    double clearRate = (visit>0)?(double)clear/visit:0.0;

                    return new EpisodeAnalyticsDto(
                            ep.getId(),
                            ep.getText(),
                            visit,
                            clear,
                            clearRate
                    );
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getRoutes(){

        return routeRepository.findAll().stream()
                .map(r-> new RouteResponse(
                        r.getId(),
                        r.getCharacter().getId(),
                        r.getCharacter().getName(),
                        r.getTitle(),
                        r.getMinAffectionRequired(),
                        r.getTrueEndingThreshold(),
                        r.getWarningText()
                )).toList();
    }


    /**
     * 특정 루트의 선택지별 선택,성공,실패 통계
     */
    @Transactional(readOnly = true)
    public List<ChoiceAnalyticsDto> getChoiceStatistics(Long routeId) {

        List<Analytics> list = analyticsRepository.findChoiceStatsByRoute(AnalyticsKind.CHOICE,routeId);

        return list.stream()
                .map(a->{
                    var choice = a.getChoice();
                    long pick = a.getPickCount()!=null?a.getPickCount():0L;
                    long pass = a.getPassCount()!=null?a.getPassCount():0L;
                    long fail = pick-pass;
                    if (fail<0) fail = 0;
                    double passRate =(pick>0)?(double)pass/fail:0.0;

                    return new ChoiceAnalyticsDto(
                            choice.getId(),
                            choice.getText(),
                            choice.getEpisode().getId(),
                            choice.getThreshold(),
                            pick,
                            pass,
                            fail,
                            passRate
                    );
                }).toList();
    }

    /**
     * 루트 자체의 엔딩 분포
     */
    @Transactional(readOnly = true)
    public RouteAnalyticsDto getRouteSummary(Long routeId) {
        Route route = routeRepository.findById(routeId).orElseThrow(()->new CustomException(ErrorCode.ROUTE_NOT_FOUND));

        List<Analytics> episodeStats = analyticsRepository.findEpisodeStatsByRoute(AnalyticsKind.EPISODE, routeId);

        long totalVisits = 0L;
        long trueClear = 0L;
        long normalClear = 0L;
        long badClear = 0L;

        // 2. 루프를 돌면서 통계를 직접 집계합니다.
        for (Analytics a : episodeStats) {
            // (1) 총 방문자 수 합산
            if (a.getVisitCount() != null) {
                totalVisits += a.getVisitCount();
            }

            // (2) 엔딩 클리어 수 합산
            Episode ep = a.getEpisode();

            // 엔딩 에피소드이고, 클리어 기록이 있다면?
            if (Boolean.TRUE.equals(ep.getIsEnding()) && a.getClearCount() != null) {
                EndingType type = ep.getEndingType();

                if (type != null) {
                    switch (type) {
                        case TRUE -> trueClear += a.getClearCount();
                        case NORMAL -> normalClear += a.getClearCount();
                        case BAD -> badClear += a.getClearCount();
                    }
                }
            }
        }

        long totalClears = trueClear + normalClear + badClear;

        return new RouteAnalyticsDto(
                route.getId(),
                route.getTitle(),
                route.getCharacter().getName(),
                totalVisits,
                totalClears,
                trueClear,
                normalClear,
                badClear
        );






    }
}
