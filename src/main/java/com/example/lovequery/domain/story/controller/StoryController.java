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
     * character get
     */
    @GetMapping("/characters")
    public List<CharacterResponse> getCharacters(){
        return storyService.getAllCharacter();
    }

    /**
     * create route
     */
    @PostMapping("/routes")
    public RouteResponse createRoute(HttpSession session, @RequestBody CreateRouteRequest req){
        Long userId = getUserId(session);
        return storyService.createRoute(userId,req);
    }
}

