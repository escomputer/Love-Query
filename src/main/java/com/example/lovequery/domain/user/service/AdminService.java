package com.example.lovequery.domain.user.service;

import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.user.entity.Role;
import com.example.lovequery.domain.user.entity.RoleRequest;
import com.example.lovequery.domain.user.entity.RoleRequestStatus;
import com.example.lovequery.domain.user.entity.User;
import com.example.lovequery.domain.user.repository.RoleRequestRepository;
import com.example.lovequery.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRequestRepository roleRequestRepository;

    private User getCurrentUser(HttpSession session){
        Long userId= (Long) session.getAttribute("userId");
        if(userId==null){
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findById(userId).orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void checkAdmin(User me){
        if(me.getRole()!= Role.GAME_ADMIN){
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }
    }

    @Transactional(readOnly = true)
    public List<RoleRequest> getPendingRequests(HttpSession session){
        User me =  getCurrentUser(session);
        checkAdmin(me);

        return roleRequestRepository.findByStatus(RoleRequestStatus.PENDING);
    }

    @Transactional
    public void approveRequest(Long requestId,HttpSession session){
        User me =  getCurrentUser(session);
        checkAdmin(me);

        RoleRequest roleRequest = roleRequestRepository.findById(requestId)
                .orElseThrow(()->new CustomException(ErrorCode.ROLE_REQUEST_NOT_FOUND));

        if(roleRequest.getStatus()!=RoleRequestStatus.PENDING){
            throw new CustomException(ErrorCode.INVALID_ROLE_REQUEST);
        }

        User target=roleRequest.getUser();
        target.changeRole(roleRequest.getRequestRole());

        roleRequest.approve();
    }

    @Transactional
    public void rejectRequest(Long requestId,HttpSession session){
        User me =  getCurrentUser(session);
        checkAdmin(me);

        RoleRequest roleRequest = roleRequestRepository.findById(requestId)
                .orElseThrow(()->new CustomException(ErrorCode.ROLE_REQUEST_NOT_FOUND));

        if(roleRequest.getStatus()!=RoleRequestStatus.PENDING){
            throw new CustomException(ErrorCode.INVALID_ROLE_REQUEST);
        }

        roleRequest.reject();
    }

}
