package com.example.lovequery.domain.user.controller;

import com.example.lovequery.domain.user.dto.RoleRequestResponse;
import com.example.lovequery.domain.user.service.AdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/role-requests")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Pending
     */
    @GetMapping
    public List<RoleRequestResponse> getRoleRequests(HttpSession session){
        return adminService.getPendingRequests(session)
                .stream()
                .map(RoleRequestResponse::from)
                .toList();
    }

    /**
     * approve
     */
    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id,HttpSession session){
        adminService.approveRequest(id,session);
    }

    /**
     * reject
     */
    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id,HttpSession session){
        adminService.rejectRequest(id,session);
    }

}
