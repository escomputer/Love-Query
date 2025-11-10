package com.example.lovequery.controller;


import com.example.lovequery.dto.User.LoginRequest;
import com.example.lovequery.dto.User.SignUpRequest;
import com.example.lovequery.entity.User;
import com.example.lovequery.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<Void> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest){

        User user = authService.login(request);

        HttpSession httpSession = httpRequest.getSession();

        httpSession.setAttribute("userId", user.getUserId());
        httpSession.setAttribute("name", user.getName());
        httpSession.setAttribute("email", user.getEmail());


        return ResponseEntity.ok().build();
    }
}
