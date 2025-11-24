package com.example.lovequery.domain.story.service;

import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameService {

    private final PlayerRepository playerRepository;
    private final GameCharacterRepository characterRepository;
    private final RouteRepository routeRepository;
    private final EpisodeRepository episodeRepository;
    private final ChoiceRepository choiceRepository;
    private final PlayerAffectionRepository playerAffectionRepository;
    private final UserRepository userRepository;

    public GameService(PlayerRepository playerRepository,
                       GameCharacterRepository characterRepository,
                       RouteRepository routeRepository,
                       EpisodeRepository episodeRepository,
                       ChoiceRepository choiceRepository,
                       PlayerAffectionRepository playerAffectionRepository, UserRepository userRepository) {
        this.playerRepository = playerRepository;
        this.characterRepository = characterRepository;
        this.routeRepository = routeRepository;
        this.episodeRepository = episodeRepository;
        this.choiceRepository = choiceRepository;
        this.playerAffectionRepository = playerAffectionRepository;
        this.userRepository = userRepository;
    }

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
    public GameStateDto startGame(Long userId, Long characterId) {
        //유저 조회(없으면 에러)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        //player 조회 (아마 회원가입한 모두가 가지고 있기에 예외처리 발생하지 않을 듯 !)
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        Route route = routeRepository.findByCharacterId(characterId).stream()
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTE_NOT_FOUND));

        Episode ep = episodeRepository.findFirstByRouteIdOrderByIdAsc(route.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.EPISODE_NOT_FOUND));

        player.changeCurrentEpisode(ep);

        playerAffectionRepository.findByPlayerIdAndCharacterId(player.getId(), characterId)
                .orElseGet(() -> playerAffectionRepository.save(new PlayerAffection(player, route.getCharacter(), 0)));

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

        //min 0
        if (update < 0) {
            update = 0;
        }

        Integer cap = thisCharacter.getAffinityCap();
        if (cap != null && update > cap) {
            update = cap;
        }

        //호감도 범위 [0,affinityCap]

        affection.updateScore(update);

        GameStateDto bad = handleRouteMinAffection(route, player, update);
        if (bad != null) return bad;

        Episode next = resolveNextEpisodeByRule(choice, update);


        //엔딩 판정 포인트
        if (Boolean.TRUE.equals(next.getIsEnding())) {
            return resolveEnding(route, player, next, update);
        }


        player.changeCurrentEpisode(next);


        return toGameStateDto(next, userId);

    }

    //호감도 부족으로 인한 badending처리
    private GameStateDto buildBadEndingState(Route route, Player player, Episode episode, int affectionScore) {
        player.changeCurrentEpisode(episode);

        List<ChoiceDto> choices = choiceRepository.findByEpisodeId(episode.getId())
                .stream()
                .map(ch->ChoiceDto.from(ch, affectionScore))
                .toList();

        return new GameStateDto(
                episode.getId(),
                episode.getText(),
                choices,
                affectionScore,
                true,
                episode.getEndingLabel(),
                "BAD"
        );
    }


    private GameStateDto handleRouteMinAffection(Route route, Player player, int updated) {
        Integer minRequired = route.getMinAffectionRequired();

        if (minRequired != null
                && updated < minRequired
                && route.getBadEndingEpisode() != null) {

            Episode bad = route.getBadEndingEpisode();
            return buildBadEndingState(route, player, bad, updated);
        }

        return null; // BAD 엔딩 아님
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
                ep.getText(),
                choiceDtos,
                score,
                isEnding,
                ep.getEndingLabel(),
                null //branch정보는 선택직후가 아니라서 없음
        );
    }

    //엔딩 이라면 분기처리
    private GameStateDto resolveEnding(Route route, Player player, Episode episode, int update) {
        Episode target;
        String endingType;

        Integer minRequired = route.getMinAffectionRequired();
        Integer trueThreshold = route.getTrueEndingThreshold();

        //True
        if (trueThreshold != null && update >= trueThreshold && route.getTrueEndingEpisode() != null) {
            target = route.getTrueEndingEpisode();
            endingType = "TRUE";

        } else if (minRequired != null && update >= minRequired && route.getNormalEndingEpisode() != null) {
            target = route.getNormalEndingEpisode();
            endingType = "NORMAL";

        } else if (route.getBadEndingEpisode() != null) {
            target = route.getBadEndingEpisode();
            endingType = "BAD";
        } else {
            target = episode;
            endingType = null;
        }

        player.changeCurrentEpisode(target);

        List<ChoiceDto> choices = choiceRepository.findByEpisodeId(target.getId())
                .stream()
                .map(choice -> ChoiceDto.from(choice,update))
                .toList();

        return new GameStateDto(
                target.getId(),
                target.getText(),
                choices,
                update,
                true,
                target.getEndingLabel(),
                endingType
        );
    }

    private Episode resolveNextEpisodeByRule(Choice choice, int update) {
        Integer threshold = choice.getThreshold();
        Episode next;

        if (threshold != null) {
            if (update >= threshold) {
                next = choice.getNextEpisodeIfPass();
                if (next == null) {
                    throw new CustomException(ErrorCode.PASS_NEXT_EPISODE_NOT_FOUND);
                }

                return next;
            } else {
                next = choice.getNextEpisodeIfFail();
                if (next == null) {
                    throw new CustomException(ErrorCode.FAIL_NEXT_EPISODE_NOT_FOUND);
                }
                return next;
            }
        }

        int delta = choice.getAffectionDelta() != null ? choice.getAffectionDelta() : 0;

        if (delta < 0) {
            next = choice.getNextEpisodeIfFail();
            if (next == null) {
                throw new CustomException(ErrorCode.FAIL_NEXT_EPISODE_NOT_FOUND);
            }
        } else {
            next = choice.getNextEpisodeIfPass();
            if (next == null) {
                throw new CustomException(ErrorCode.PASS_NEXT_EPISODE_NOT_FOUND);
            }
        }

        return next;
    }
}
