package com.example.lovequery.domain.log.controller;

import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.log.dto.PersonalReportDto;
import com.example.lovequery.domain.log.service.PlayerReportService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game/report")
@RequiredArgsConstructor
public class PlayerReportController {


    private final PlayerReportService playerReportService;

    /**
     * 플레이어 가장 최근 리포트
     */
    @GetMapping("latest")
    public PersonalReportDto getLatestReport(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }

        return playerReportService.getLatestReport(userId);
    }
}
