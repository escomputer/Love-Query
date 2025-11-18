package com.example.lovequery.domain.story.controller;

import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.story.dto.GameStateDto;
import com.example.lovequery.domain.story.service.GameService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * 게임시작/캐릭터 선택
     */
    @PostMapping("/start")
    public GameStateDto startGame(@RequestParam Long characterId, HttpSession session){
        Long userId = (Long) session.getAttribute("userId");

        if(userId == null){
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }

        return gameService.startGame(characterId,userId);
    }

    /**
     * 현재 상태 조회
     */
    @GetMapping("/current")
    public GameStateDto getCurrentGame(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }

        return gameService.getCurrentState(userId);
    }

    /**
     * 선택지 선택
     */
    @PostMapping("/choose")
    public GameStateDto choose(@RequestBody Long choiceId,HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }

        return gameService.choose(userId, choiceId);
    }
}
