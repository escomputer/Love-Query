package com.example.lovequery.domain.log.service;

import com.example.lovequery.common.EndingType;
import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.log.dto.PersonalReportDto;
import com.example.lovequery.domain.log.dto.PlayStepDto;
import com.example.lovequery.domain.log.entity.PlayLog;
import com.example.lovequery.domain.log.repository.PlayLogRepository;
import com.example.lovequery.domain.player.entity.Player;
import com.example.lovequery.domain.player.repository.PlayerRepository;
import com.example.lovequery.domain.story.entity.Episode;
import com.example.lovequery.domain.story.entity.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerReportService {

    private final PlayerRepository playerRepository;
    private final PlayLogRepository playLogRepository;

    /**
     * 가장 최근 플레이 세션 기준 리포트
     */
    @Transactional(readOnly = true)
    public PersonalReportDto getLatestReport(Long userId) {

        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        PlayLog latestlog = playLogRepository.findTopByPlayerIdOrderByCreatedAtDesc(player.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYER_LOG_NOT_FOUND));

        String sessionId = latestlog.getSessionId();
        Long routeId = latestlog.getRoute().getId();

        List<PlayLog> logs = playLogRepository.findByRouteIdAndPlayerIdAndSessionIdOrderByCreatedAtAsc(
                routeId, player.getId(), sessionId
        );

        if (logs.isEmpty()) {
            throw new CustomException(ErrorCode.PLAYER_LOG_NOT_FOUND);
        }

        Route route = logs.get(0).getRoute();

        PlayLog finalLog = logs.get(logs.size() - 1);
        int finalAffection = finalLog.getAffectionAfter();

        EndingType endingType = resolveEndingtype(route, finalAffection);
        String endingLabel = resolveEndingLabel(route, endingType);

        List<PlayStepDto> steps = logs.stream()
                .sorted(Comparator.comparing(PlayLog::getCreatedAt))
                .map(log->{
                    Episode ep = log.getEpisode();
                    String choiceText = log.getChoice()!=null?
                            log.getChoice().getText():null;

                    return new PlayStepDto(
                            ep.getId(),
                            ep.getText(),
                            log.getChoice()!=null?log.getChoice().getId():null,
                            choiceText,
                            log.getAffectionBefore(),
                            log.getAffectionAfter()
                    );
                }).toList();

        return new PersonalReportDto(
                routeId,
                route.getTitle(),
                route.getCharacter().getName(),
                finalAffection,
                endingType,
                endingLabel
                ,steps
        );

    }

    private EndingType resolveEndingtype(Route route, int affection) {
        Integer minRequired = route.getMinAffectionRequired();
        Integer trueThreshold = route.getTrueEndingThreshold();


        if (trueThreshold != null && affection >= trueThreshold && route.getTrueEndingEpisode() != null) {
            return EndingType.TRUE;
        } else if (minRequired != null && affection >= minRequired && route.getNormalEndingEpisode() != null) {
            return EndingType.NORMAL;
        } else if (route.getBadEndingEpisode() != null) {
            return EndingType.BAD;
        } else {
            return null;
        }
    }

    private String resolveEndingLabel(Route route, EndingType endingType) {
        if (endingType == null) {
            return null;
        }

        return switch (endingType) {
            case TRUE -> route.getTrueEndingEpisode()!=null ?
                        route.getTrueEndingEpisode().getEndingLabel():null;
            case BAD -> route.getBadEndingEpisode()!=null ?
                        route.getBadEndingEpisode().getEndingLabel():null;
            case NORMAL ->  route.getNormalEndingEpisode()!=null ?
                        route.getNormalEndingEpisode().getEndingLabel():null;
        };
    }

}
