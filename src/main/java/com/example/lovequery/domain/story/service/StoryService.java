package com.example.lovequery.domain.story.service;

import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.analytics.repository.AnalyticsRepository;
import com.example.lovequery.domain.log.entity.AdminLog;
import com.example.lovequery.domain.log.repository.AdminLogRepository;
import com.example.lovequery.domain.log.repository.PlayLogRepository;
import com.example.lovequery.domain.player.repository.PlayerAffectionRepository;
import com.example.lovequery.domain.player.repository.PlayerRepository;
import com.example.lovequery.domain.story.dto.character.CharacterResponse;
import com.example.lovequery.domain.story.dto.character.CreateCharacterRequest;
import com.example.lovequery.domain.story.dto.character.UpdateCharacterRequest;
import com.example.lovequery.domain.story.dto.choice.ChoiceResponse;
import com.example.lovequery.domain.story.dto.choice.CreateChoiceRequest;
import com.example.lovequery.domain.story.dto.choice.UpdateChoiceRequest;
import com.example.lovequery.domain.story.dto.ep.CreateEpisodeRequest;
import com.example.lovequery.domain.story.dto.ep.EpisodeResponse;
import com.example.lovequery.domain.story.dto.ep.UpdateEpisodeRequest;
import com.example.lovequery.domain.story.dto.route.CreateRouteRequest;
import com.example.lovequery.domain.story.dto.route.RouteResponse;
import com.example.lovequery.domain.story.dto.route.UpdateRouteRequest;
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
    private final AnalyticsRepository analyticsRepository;
    private final PlayLogRepository playLogRepository;
    private final PlayerAffectionRepository playerAffectionRepository;
    private final PlayerRepository playerRepository;
    private final AdminLogRepository adminLogRepository;


    private void saveAdminLog(Long userId, String action, String targetType, Long targetId, String desc) {
        adminLogRepository.save(new AdminLog(userId, action, targetType, targetId, desc));
    }
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

        Episode episode = new Episode(route, request.title(), request.text(), request.isEnding(), request.endingType());

        Episode saved = episodeRepository.save(episode);

        saveAdminLog(userId, "CREATE", "EPISODE", saved.getId(), "에피소드 생성");

        return new EpisodeResponse(
                saved.getId(),
                saved.getRoute().getId(),
                saved.getTitle(),
                saved.getText(),
                saved.getIsEnding(),
                saved.getEndingType()
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

        if(request.nextEpIfPassId()!= null){
            nextPass=episodeRepository.findById(request.nextEpIfPassId())
                    .orElseThrow(()->new CustomException(ErrorCode.PASS_EPISODE_NOT_FOUND));
        }

        int delta = request.affectionDelta()==null?0:request.affectionDelta();
        Integer threshold=request.threshold();
        Integer minAffection = request.minRequiredAffection();

        Choice choice = new Choice(
                ep,
                request.text(),
                nextFail,
                nextPass,
                delta,
                threshold,
                minAffection
        );

        Choice saved = choiceRepository.save(choice);

        saveAdminLog(userId, "CREATE", "CHOICE", saved.getId(), "선택지 생성: " + saved.getText());

        return new ChoiceResponse(
                saved.getId(),
                saved.getEpisode().getId(),
                saved.getText(),
                saved.getNextEpisodeIfFail()!=null?saved.getNextEpisodeIfFail().getId():null,
                saved.getNextEpisodeIfPass()!=null?saved.getNextEpisodeIfPass().getId():null,
                saved.getAffectionDelta(),
                saved.getThreshold(),
                saved.getMinRequiredAffection()
        );
    }

    @Transactional
    public CharacterResponse createCharacter(Long userId, CreateCharacterRequest request){
        getWriterOrAdmin(userId);

        int finalCap = (request.affinityCap() != null) ? request.affinityCap() : 100;

        GameCharacter character = new GameCharacter(
                request.name(),
                request.gender(),
                request.personality(),
                finalCap
        );

        GameCharacter saved = gameCharacterRepository.save(character);

        saveAdminLog(userId, "CREATE", "CHARACTER", saved.getId(), "캐릭터 생성: " + saved.getName());

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

        GameCharacter character = gameCharacterRepository.findById(request.characterId())
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        Route route = new Route(
                character,
                request.title(),
                request.minAffectionRequired(),
                request.trueEndingThreshold(),
                request.warningText()
        );

        Route saved = routeRepository.save(route);

        saveAdminLog(userId, "CREATE", "ROUTE", saved.getId(), "루트 생성: " + saved.getTitle());

        return new RouteResponse(
                saved.getId(),
                saved.getCharacter().getId(),
                saved.getCharacter().getName(),
                saved.getTitle(),
                saved.getMinAffectionRequired(),
                saved.getTrueEndingThreshold(),
                saved.getWarningText()
        );

    }

    

    //  캐릭터 수정
    @Transactional
    public void updateCharacter(Long userId, Long charId, UpdateCharacterRequest req) {
        getWriterOrAdmin(userId);
        GameCharacter character = gameCharacterRepository.findById(charId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));
        int finalCap = (req.affinityCap() != null) ? req.affinityCap() : 100;
        character.update(req.name(), req.gender(), req.personality(),finalCap);

        saveAdminLog(userId, "UPDATE", "CHARACTER", charId, "정보 수정");
    }

    // 루트 수정
    @Transactional
    public void updateRoute(Long userId, Long routeId, UpdateRouteRequest req) {
        getWriterOrAdmin(userId);
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTE_NOT_FOUND));
        if (req.startEpisodeId() != null) {
            Episode startEp = episodeRepository.findById(req.startEpisodeId())
                    .orElseThrow(() -> new CustomException(ErrorCode.EPISODE_NOT_FOUND));
            if (!startEp.getRoute().getId().equals(routeId)) {
                throw new CustomException(ErrorCode.NO_PERMISSION, "다른 루트의 에피소드를 시작점으로 설정할 수 없습니다.");
            }
        }
        route.update(req.title(), req.minAffectionRequired(), req.startEpisodeId(), req.trueEndingThreshold(), req.warningText());

        saveAdminLog(userId, "UPDATE", "ROUTE", routeId, "정보 수정");
    }

    // 에피소드 수정
    @Transactional
    public void updateEpisode(Long userId, Long epId, UpdateEpisodeRequest req) {
        getWriterOrAdmin(userId);
        Episode episode = episodeRepository.findById(epId)
                .orElseThrow(() -> new CustomException(ErrorCode.EPISODE_NOT_FOUND));

        // endingType 로직 반영
        episode.update(req.title(), req.text(), req.isEnding(), req.endingType());
        saveAdminLog(userId, "UPDATE", "EPISODE", epId, "정보 수정");
    }

    // 선택지 수정
    @Transactional
    public void updateChoice(Long userId, Long choiceId, UpdateChoiceRequest req) {
        getWriterOrAdmin(userId);
        Choice choice = choiceRepository.findById(choiceId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHOICE_NOT_FOUND));

        Episode nextPass = (req.nextEpIfPassId() != null) ?
                episodeRepository.findById(req.nextEpIfPassId()).orElseThrow() : null;
        Episode nextFail = (req.nextEpIfFailId() != null) ?
                episodeRepository.findById(req.nextEpIfFailId()).orElseThrow() : null;

        choice.update(
                req.text(), nextPass, nextFail,
                req.threshold(),
                req.affectionDelta() != null ? req.affectionDelta() : 0,
                req.minRequiredAffection()
        );

        saveAdminLog(userId, "UPDATE", "CHOICE", choiceId, "정보 수정");
    }

    //조회 메서드들

    @Transactional(readOnly = true)
    public List<RouteResponse> getRoutesByCharacter(Long userId, Long characterId) {

        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        List<Route> route= routeRepository.findByCharacterId(character.getId());

        return route.stream()
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

    @Transactional(readOnly = true)
    public List<EpisodeResponse> getEpisodesByRoute(Long userId, Long routeId){
        getWriterOrAdmin(userId);

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROUTE_NOT_FOUND));

        return episodeRepository.findByRouteIdOrderByIdAsc(routeId).stream()
                .map(ep-> new EpisodeResponse(
                        ep.getId(),
                        routeId,
                        ep.getTitle(),
                        ep.getText(),
                        ep.getIsEnding(),
                        ep.getEndingType()
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<ChoiceResponse> getChoicesByEpisode(Long userId, Long epId){
        getWriterOrAdmin(userId);

        Episode ep = episodeRepository.findById(epId)
                .orElseThrow(() -> new CustomException(ErrorCode.EPISODE_NOT_FOUND));

        return choiceRepository.findByEpisodeId(epId).stream()
                .map(ch-> new ChoiceResponse(
                        ch.getId(),
                        epId,
                        ch.getText(),
                        ch.getNextEpisodeIfFail()!=null?ch.getNextEpisodeIfFail().getId():null,
                        ch.getNextEpisodeIfPass()!=null?ch.getNextEpisodeIfPass().getId():null,
                        ch.getAffectionDelta(),
                        ch.getThreshold(),
                        ch.getMinRequiredAffection()
                )).toList();
    }
    
    //delete

    @Transactional
    public void deleteCharacter(Long userId, Long charId) {
        getWriterOrAdmin(userId);


        List<Route> routes = routeRepository.findByCharacterId(charId);
        for (Route r : routes) {
            deleteRoute(userId, r.getId());
        }


        playerAffectionRepository.deleteByCharacterId(charId);


        gameCharacterRepository.deleteById(charId);

        saveAdminLog(userId, "DELETE", "CHARACTER", charId, "캐릭터 및 하위 데이터 삭제");
    }


    @Transactional
    public void deleteRoute(Long userId, Long routeId) {
        getWriterOrAdmin(userId);


        List<Episode> episodes = episodeRepository.findByRouteIdOrderByIdAsc(routeId);
        for (Episode ep : episodes) {
            deleteEpisode(userId, ep.getId());
        }


        playLogRepository.deleteByRouteId(routeId);

        routeRepository.deleteById(routeId);

        saveAdminLog(userId, "DELETE", "ROUTE", routeId, "route 및 하위 데이터 삭제");
    }

    @Transactional
    public void deleteEpisode(Long userId, Long epId) {
        getWriterOrAdmin(userId);


        List<Choice> choices = choiceRepository.findByEpisodeId(epId);
        for (Choice c : choices) {
            deleteChoice(userId, c.getId());
        }


        analyticsRepository.deleteByEpisodeId(epId);
        playLogRepository.deleteByEpisodeId(epId);


        playerRepository.detachEpisodeFromPlayers(epId);


        episodeRepository.deleteById(epId);

        saveAdminLog(userId, "DELETE", "EPISODE", epId, "episode 및 하위 데이터 삭제 + 유저 강퇴");
    }


    @Transactional
    public void deleteChoice(Long userId, Long choiceId) {
        getWriterOrAdmin(userId);


        analyticsRepository.deleteByChoiceId(choiceId);


        playLogRepository.deleteByChoiceId(choiceId);


        choiceRepository.deleteById(choiceId);

        saveAdminLog(userId, "DELETE", "CHOICE", choiceId, "choice 및 하위 데이터 삭제");
    }

}
