package com.example.lovequery.domain.user.controller;


import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.user.dto.LoginRequest;
import com.example.lovequery.domain.user.dto.LoginResponse;
import com.example.lovequery.domain.user.dto.SignUpRequest;
import com.example.lovequery.domain.user.dto.UserInfoResponse;
import com.example.lovequery.domain.user.entity.User;
import com.example.lovequery.domain.user.repository.UserRepository;
import com.example.lovequery.domain.user.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;


    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignUpRequest request){
        authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpSession session){

        return authService.login(request,session);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session){
        authService.logout(session);
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 로그인한 정보
     */
    @GetMapping("/me")
    public UserInfoResponse getCurrentUser(HttpSession session){
        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            throw new CustomException(ErrorCode.NO_PERMISSION);
        }

        User user= userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}
