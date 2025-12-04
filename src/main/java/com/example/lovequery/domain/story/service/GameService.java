package com.example.lovequery.domain.story.service;

import com.example.lovequery.common.EndingType;
import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.analytics.service.AnalyticsService;
import com.example.lovequery.domain.log.entity.PlayLog;
import com.example.lovequery.domain.log.repository.PlayLogRepository;
import com.example.lovequery.domain.player.entity.Player;
import com.example.lovequery.domain.player.entity.PlayerAffection;
import com.example.lovequery.domain.player.repository.PlayerAffectionRepository;
import com.example.lovequery.domain.player.repository.PlayerRepository;
import com.example.lovequery.domain.story.dto.GameStateDto;
import com.example.lovequery.domain.story.dto.character.CharacterResponse;
import com.example.lovequery.domain.story.dto.choice.ChoiceDto;
import com.example.lovequery.domain.story.entity.Choice;
import com.example.lovequery.domain.story.entity.Episode;
import com.example.lovequery.domain.story.entity.GameCharacter;
import com.example.lovequery.domain.story.entity.Route;
import com.example.lovequery.domain.story.repository.ChoiceRepository;
import com.example.lovequery.domain.story.repository.EpisodeRepository;
import com.example.lovequery.domain.story.repository.GameCharacterRepository;
import com.example.lovequery.domain.story.repository.RouteRepository;
import com.example.lovequery.domain.user.entity.User;
import com.example.lovequery.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GameService {

    private final PlayerRepository playerRepository;
    private final GameCharacterRepository characterRepository;
    private final RouteRepository routeRepository;
    private final EpisodeRepository episodeRepository;
    private final ChoiceRepository choiceRepository;
    private final PlayerAffectionRepository playerAffectionRepository;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final PlayLogRepository playLogRepository;

    @Transactional(readOnly = true)
    public List<CharacterResponse> getAllCharacter() {
        return characterRepository.findAll().stream()
                .map(c -> new CharacterResponse(
                        c.getId(),
                        c.getName(),
                        c.getGender(),
                        c.getPersonality(),
                        c.getAffinityCap(),
                        c.getPopularityScore()
                ))
                .toList();
    }

    @Transactional
    public GameStateDto startGame(Long userId, Long routeId) {
        //유저 조회(없으면 에러)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        //player 조회 (아마 회원가입한 모두가 가지고 있기에 예외처리 발생하지 않을 듯 !)
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        String newSessionId = UUID.randomUUID().toString();
        player.changeCurrentSessionId(newSessionId);

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTE_NOT_FOUND));

        GameCharacter character = route.getCharacter();
        character.increasePopularity();

        Episode ep = episodeRepository.findById(route.getStartEpisodeId())
                .orElseThrow(() -> new CustomException(ErrorCode.EPISODE_NOT_FOUND));

        player.changeCurrentEpisode(ep);

        playerAffectionRepository.findByPlayerIdAndCharacterId(player.getId(), character.getId())
                .orElseGet(() -> playerAffectionRepository.save(new PlayerAffection(player, route.getCharacter(), 50)));

        analyticsService.recordEpVisit(ep);

        return toGameStateDto(ep, userId);


    }

    @Transactional(readOnly = true)
    public GameStateDto getCurrentState(Long userId) {

        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        Episode current = player.getCurrentEpisode();

        if (current == null) {
            throw new CustomException(ErrorCode.EPISODE_NOT_FOUND);
        }

        return toGameStateDto(current, userId);

    }

    @Transactional
    public GameStateDto choose(Long userId, Long choiceId) {

        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        Choice choice = choiceRepository.findById(choiceId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHOICE_NOT_FOUND));

        Episode current = player.getCurrentEpisode();

        if (current == null) {
            throw new CustomException(ErrorCode.EPISODE_NOT_FOUND);
        }

        Route route = current.getRoute();
        GameCharacter thisCharacter = route.getCharacter();

        // affection 조회
        PlayerAffection affection = playerAffectionRepository
                .findByPlayerIdAndCharacterId(player.getId(), thisCharacter.getId())
                .orElseGet(() -> playerAffectionRepository.save(
                        new PlayerAffection(player, thisCharacter, 0)
                ));

        int currentScore = affection.getScore();

        Integer minAffection = choice.getMinRequiredAffection();
        if (minAffection != null && currentScore < minAffection) {
            throw new CustomException(ErrorCode.CHOICE_LOCKED, "호감도" + minAffection + "이상이어야 선택할 수 있습니다.");
        }

        //affectionDelta 적용하기
        int before = currentScore;
        int delta = choice.getAffectionDelta() != null ? choice.getAffectionDelta() : 0;
        int update = before + delta;



        Integer cap = thisCharacter.getAffinityCap();
        if (cap != null && update > cap) {
            update = cap;
        }

        //호감도 범위 [0,affinityCap]

        affection.updateScore(update);

        saveLog(player, route, current, choice, before, update);

        Episode next = null;


        Integer minRequired = route.getMinAffectionRequired();

        if (minRequired != null && update < minRequired) {
            // DB에서 이 루트의 'BAD' 엔딩 에피소드를 찾아옴
            next = episodeRepository.findFirstByRouteIdAndEndingType(route.getId(), EndingType.BAD)
                    .orElseThrow(() -> new CustomException(ErrorCode.BAD_EPISODE_NOT_FOUND));
        } else {
            // 만약 threshold 로직을 쓴다면 여기서 처리
            if (choice.getThreshold() != null && update < choice.getThreshold()) {
                next = choice.getNextEpisodeIfFail(); // 실패 경로
            } else {
                next = choice.getNextEpisodeIfPass(); // 성공 경로 (기본)
            }

            if (next == null) {
                throw new CustomException(ErrorCode.EPISODE_NOT_FOUND, "다음 에피소드가 연결되지 않았습니다.");
            }

            if (next.getEndingType() == EndingType.TRUE) {
                Integer trueCutline = route.getTrueEndingThreshold();

                // 컷라인이 있고 점수가 부족하면 -> NORMAL로 변경
                if (trueCutline != null && update < trueCutline) {
                    next = episodeRepository.findFirstByRouteIdAndEndingType(route.getId(), EndingType.NORMAL)
                            .orElseThrow(() -> new CustomException(ErrorCode.NORMAL_EPISODE_NOT_FOUND));
                }
            }
        }


        analyticsService.recordChoice(choice, true);
        analyticsService.recordEpVisit(next);

        if (Boolean.TRUE.equals(next.getIsEnding())) {
            analyticsService.recordEndingClear(next);
            saveLog(player, route, next, null, update, update);
        }

        player.changeCurrentEpisode(next);

        // DTO 반환 (이제 endingLabel 대신 endingType 사용)
        return toGameStateDto(next, userId);
    }



    private GameStateDto toGameStateDto(Episode ep, Long userId) {


        //호감도 불러오기 없으면 0

        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        GameCharacter character = ep.getRoute().getCharacter();

        PlayerAffection affection = playerAffectionRepository.findByPlayerIdAndCharacterId(
                player.getId(), character.getId()
        ).orElse(null);

        int score = (affection != null) ? affection.getScore() : 0;
        List<ChoiceDto> choiceDtos = choiceRepository.findByEpisodeId(ep.getId())
                .stream()
                .map(ch->ChoiceDto.from(ch, score))
                .toList();

        boolean isEnding = Boolean.TRUE.equals(ep.getIsEnding());

        return new GameStateDto(
                ep.getId(),
                ep.getTitle(),
                ep.getText(),
                choiceDtos,
                score,
                isEnding,
                ep.getEndingType()
        );
    }



    private void saveLog(Player player, Route route, Episode episode,Choice choice, int affectionBefore, int affectionAfter) {
        String sessionId = player.getCurrentSessionId();

        PlayLog playLog = new PlayLog(
                sessionId,
                player,
                route,
                episode,
                choice,
                affectionBefore,
                affectionAfter
        );

        playLogRepository.save(playLog);
    }

    @Transactional
    public void resetRoute(Long userId){
        Player player = playerRepository.findByUserId(userId).orElseThrow(()->new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        Episode current = player.getCurrentEpisode();
        if (current != null) {
            Route route = current.getRoute();
            GameCharacter character = route.getCharacter();
            playerAffectionRepository.findByPlayerIdAndCharacterId(player.getId(), character.getId())
                    .ifPresent(affection -> affection.updateScore(50)); // 혹은 삭제
        }

        player.changeCurrentSessionId(null);
        player.changeCurrentEpisode(null);

        // [중요] JPA가 트랜잭션 끝날 때 알아서 하겠지만, 확실하게 하기 위해 저장!
        playerRepository.save(player);
    }


}
