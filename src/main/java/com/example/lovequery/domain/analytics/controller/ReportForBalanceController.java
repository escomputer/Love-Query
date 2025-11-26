package com.example.lovequery.domain.analytics.controller;

import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.analytics.dto.ChoiceAnalyticsDto;
import com.example.lovequery.domain.analytics.dto.EpisodeAnalyticsDto;
import com.example.lovequery.domain.analytics.dto.RouteAnalyticsDto;
import com.example.lovequery.domain.analytics.service.ReportForBalanceService;
import com.example.lovequery.domain.story.dto.route.RouteResponse;
import com.example.lovequery.domain.user.entity.Role;
import com.example.lovequery.domain.user.entity.User;
import com.example.lovequery.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class ReportForBalanceController {

    private final ReportForBalanceService reportForBalanceService;
    private final UserRepository userRepository;


    /**
     * 권한체크
     */
    private void checkAdminOrTuner(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.GAME_ADMIN &&
                user.getRole() != Role.BALANCE_TUNER) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
    }

    @GetMapping("/routes")
    public List<RouteResponse> getRoutes(HttpSession httpSession) {
        Long userId = (Long) httpSession.getAttribute("userId");
        if (userId == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        checkAdminOrTuner(userId);

        return reportForBalanceService.getRoutes();

    }

    /**
     * route summary stats
     */
    @GetMapping("/routes/{routeId}/summary")
    public RouteAnalyticsDto getRouteSummary(@PathVariable Long routeId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
        checkAdminOrTuner(userId);

        return reportForBalanceService.getRouteSummary(routeId);

    }

    /**
     * ep summary stats
     */
    @GetMapping("/routes/{routeId}/episodes")
    public List<EpisodeAnalyticsDto> getEpisodeSummary(@PathVariable Long routeId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
        checkAdminOrTuner(userId);

        return reportForBalanceService.getEpisodeStatistics(routeId);
    }

    /**
     * choice summary stats
     *
     */
    @GetMapping("/routes/{routeId}/choices")
    public List<ChoiceAnalyticsDto> getChoiceSummary(@PathVariable Long routeId, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
        checkAdminOrTuner(userId);
        return reportForBalanceService.getChoiceStatistics(routeId);
    }
}
