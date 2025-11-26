package com.example.lovequery.domain.story.controller;


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
import com.example.lovequery.domain.story.service.StoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/story")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    private Long getUserId(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");

        if(userId == null){
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }

        return userId;
    }


    /**
     * episode create
     */
    @PostMapping("/episodes")
    public EpisodeResponse createEpisode(HttpSession session, @RequestBody CreateEpisodeRequest req){
        Long userId = getUserId(session);

        return storyService.createEpisode(userId,req);
    }

    /**
     * choice create
     */
    @PostMapping("/episodes/{epId}/choices")
    public ChoiceResponse createChoice(HttpSession session, @PathVariable Long epId, @RequestBody CreateChoiceRequest req){
        Long userId = getUserId(session);
        return storyService.createChoice(userId,epId,req);
    }

    /**
     * character create
     *
     */
    @PostMapping("/characters")
    public CharacterResponse createCharacter(HttpSession session, @RequestBody CreateCharacterRequest req){
        Long userId = getUserId(session);
        return storyService.createCharacter(userId,req);
    }

    /**
     * 특정 캐릭터의 route리스트 조회
     */
    @GetMapping("/characters/{characterId}/routes")
    public List<RouteResponse> getRoutesByCharacter(HttpSession session, @PathVariable Long characterId){
        Long userId = getUserId(session);

        return storyService.getRoutesByCharacter(userId,characterId);
    }



    /**
     * create route
     */
    @PostMapping("/routes")
    public RouteResponse createRoute(HttpSession session, @RequestBody CreateRouteRequest req){
        Long userId = getUserId(session);
        return storyService.createRoute(userId,req);
    }

    /**
     * Route별 Episode 리스트 조회
     */
    @GetMapping("/routes/{routeId}/episodes")
    public List<EpisodeResponse> getEpisodesByRoute(@PathVariable Long routeId,
                                                    HttpSession session) {

        Long userId = getUserId(session);
        return storyService.getEpisodesByRoute(userId, routeId);
    }

    /**
     * Episode별 Choice 리스트 조회
     */
    @GetMapping("/episodes/{epId}/choices")
    public List<ChoiceResponse> getChoicesByEpisode(@PathVariable Long epId,
                                                    HttpSession session) {

        Long userId = getUserId(session);
        return storyService.getChoicesByEpisode(userId, epId);
    }


}

