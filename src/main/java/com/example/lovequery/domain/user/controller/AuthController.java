package com.example.lovequery.domain.user.controller;


import com.example.lovequery.domain.user.dto.LoginRequest;
import com.example.lovequery.domain.user.dto.LoginResponse;
import com.example.lovequery.domain.user.dto.SignUpRequest;
import com.example.lovequery.domain.user.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


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
}
