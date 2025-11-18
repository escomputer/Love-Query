package com.example.lovequery.domain.story.service;

import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.player.entity.Player;
import com.example.lovequery.domain.player.entity.PlayerAffection;
import com.example.lovequery.domain.player.repository.PlayerAffectionRepository;
import com.example.lovequery.domain.player.repository.PlayerRepository;
import com.example.lovequery.domain.story.dto.choice.ChoiceDto;
import com.example.lovequery.domain.story.dto.character.GameCharacterDto;
import com.example.lovequery.domain.story.dto.GameStateDto;
import com.example.lovequery.domain.story.entity.Choice;
import com.example.lovequery.domain.story.entity.Episode;
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

    public List<GameCharacterDto> getCharacters() {
        return characterRepository.findAll().stream()
                .map(GameCharacterDto::from)
                .toList();
    }

    @Transactional
    public GameStateDto startGame(Long userId, Long characterId){
        //유저 조회(없으면 에러)
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

        //player 조회 (아마 회원가입한 모두가 가지고 있기에 예외처리 발생하지 않을 듯 !)
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(()-> new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        Route route = routeRepository.findByCharacterId(characterId).stream()
                .findFirst()
                .orElseThrow(()->new CustomException(ErrorCode.ROUTE_NOT_FOUND));

        Episode ep = episodeRepository.findFirstByRouteIdOrderByIdAsc(route.getId())
                .orElseThrow(()->new CustomException(ErrorCode.EPISODE_NOT_FOUND));

        player.changeCurrentEpisode(ep);

        playerAffectionRepository.findByPlayerIdAndCharacterId(player.getId(), characterId)
                .orElseGet(()->playerAffectionRepository.save(new PlayerAffection(player,route.getCharacter(),0)));

        return toGameStateDto(ep);


    }

    @Transactional(readOnly = true)
    public GameStateDto getCurrentState(Long userId){

        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(()-> new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        Episode current = player.getCurrentEpisode();

        if(current == null){
            throw new CustomException(ErrorCode.EPISODE_NOT_FOUND);
        }

        return toGameStateDto(current);

    }

    @Transactional
    public GameStateDto choose(Long userId, Long choiceId){

        Player player=playerRepository.findByUserId(userId)
                .orElseThrow(()->new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        Choice choice = choiceRepository.findById(choiceId)
                .orElseThrow(()->new CustomException(ErrorCode.CHOICE_NOT_FOUND));

        //TODO: affectionDelta, threshold,pass/fail 로직 나중에 구현

        Episode next = choice.getNextEpisodeIfPass();
        if(next == null){
            throw new CustomException(ErrorCode.EPISODE_NOT_FOUND);
        }

        player.changeCurrentEpisode(next);

        return toGameStateDto(next);

    }




    private GameStateDto toGameStateDto(Episode ep) {
        List<ChoiceDto> choiceDtos= choiceRepository.findByEpisodeId(ep.getId())
                .stream()
                .map(ChoiceDto::from)
                .toList();

        return new GameStateDto(
                ep.getId(),
                ep.getText(),
                choiceDtos
        );
    }
}
