package com.example.lovequery.domain.story.service;

import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.story.dto.character.CharacterResponse;
import com.example.lovequery.domain.story.dto.character.CreateCharacterRequest;
import com.example.lovequery.domain.story.dto.choice.ChoiceResponse;
import com.example.lovequery.domain.story.dto.choice.CreateChoiceRequest;
import com.example.lovequery.domain.story.dto.ep.CreateEpisodeRequest;
import com.example.lovequery.domain.story.dto.ep.EpisodeResponse;
import com.example.lovequery.domain.story.dto.route.CreateRouteRequest;
import com.example.lovequery.domain.story.dto.route.RouteResponse;
import com.example.lovequery.domain.story.entity.Choice;
import com.example.lovequery.domain.story.entity.Episode;
import com.example.lovequery.domain.story.entity.GameCharacter;
import com.example.lovequery.domain.story.entity.Route;
import com.example.lovequery.domain.story.repository.ChoiceRepository;
import com.example.lovequery.domain.story.repository.EpisodeRepository;
import com.example.lovequery.domain.story.repository.GameCharacterRepository;
import com.example.lovequery.domain.story.repository.RouteRepository;
import com.example.lovequery.domain.user.entity.Role;
import com.example.lovequery.domain.user.entity.User;
import com.example.lovequery.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final EpisodeRepository episodeRepository;
    private final ChoiceRepository choiceRepository;
    private final GameCharacterRepository gameCharacterRepository;

    /**
     * 권한체크
     */

    private User getWriterOrAdmin(Long userId){
        User user= userRepository.findById(userId)
                .orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND));

        if(user.getRole()!= Role.GAME_ADMIN && user.getRole()!=Role.SCENARIO_WRITER ){
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
        return user;
    }

    @Transactional
    public EpisodeResponse createEpisode(Long userId, CreateEpisodeRequest request){
        getWriterOrAdmin(userId);

        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(()->new CustomException(ErrorCode.ROUTE_NOT_FOUND));

        Episode episode = new Episode(route,request.text());

        Episode saved = episodeRepository.save(episode);

        return new EpisodeResponse(
                saved.getId(),
                saved.getRoute().getId(),
                saved.getText()
        );
    }

    @Transactional
    public ChoiceResponse createChoice(Long userId, Long epId,CreateChoiceRequest request){
        getWriterOrAdmin(userId);

        Episode ep = episodeRepository.findById(epId)
                .orElseThrow(()->new CustomException(ErrorCode.EPISODE_NOT_FOUND));

        Episode nextPass = null;
        Episode nextFail = null;

        if(request.nextEpIfFailId() != null){
            nextFail = episodeRepository.findById(request.nextEpIfFailId())
                    .orElseThrow(()->new CustomException(ErrorCode.FAIL_EPISODE_NOT_FOUND));
        }

        if(request.nextEpIfSuccId()!= null){
            nextPass=episodeRepository.findById(request.nextEpIfSuccId())
                    .orElseThrow(()->new CustomException(ErrorCode.PASS_EPISODE_NOT_FOUND));
        }

        int delta = request.affectionDelta()==null?0:request.affectionDelta();
        Integer threshold=request.threshold();

        Choice choice = new Choice(
                ep,
                request.text(),
                nextFail,
                nextPass,
                delta,
                threshold
        );

        Choice saved = choiceRepository.save(choice);

        return new ChoiceResponse(
                saved.getId(),
                saved.getEpisode().getId(),
                saved.getText(),
                saved.getNextEpisodeIfFail()!=null?saved.getNextEpisodeIfFail().getId():null,
                saved.getNextEpisodeIfPass()!=null?saved.getNextEpisodeIfPass().getId():null,
                saved.getAffectionDelta(),
                saved.getThreshold()
        );
    }

    @Transactional
    public CharacterResponse createCharacter(Long userId, CreateCharacterRequest request){
        getWriterOrAdmin(userId);

        GameCharacter character = new GameCharacter(
                request.name(),
                request.gender(),
                request.personality(),
                request.affinityCap()
        );

        GameCharacter saved = gameCharacterRepository.save(character);

        return new CharacterResponse(
                saved.getId(),
                saved.getName(),
                saved.getGender(),
                saved.getPersonality(),
                saved.getAffinityCap(),
                saved.getPopularityScore()
        );


    }



    @Transactional
    public RouteResponse createRoute(Long userId, CreateRouteRequest request){
        getWriterOrAdmin(userId);

        GameCharacter character = gameCharacterRepository.findById(request.charId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        Route route = new Route(
                character,
                request.title(),
                request.minAffectionRequired(),
                request.warningText()
        );

        Route saved = routeRepository.save(route);

        return new RouteResponse(
                saved.getId(),
                saved.getCharacter().getId(),
                saved.getCharacter().getName(),
                saved.getTitle(),
                saved.getMinAffectionRequired(),
                saved.getWarningText()
        );

    }
}
