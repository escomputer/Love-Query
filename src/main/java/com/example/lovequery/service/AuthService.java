package com.example.lovequery.service;

import com.example.lovequery.dto.User.LoginRequest;
import com.example.lovequery.dto.User.SignUpRequest;
import com.example.lovequery.entity.User;
import com.example.lovequery.exception.EmailDuplicateException;
import com.example.lovequery.exception.LoginFailedException;
import com.example.lovequery.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;


    @Transactional
    public void signUp(SignUpRequest request) {
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new EmailDuplicateException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .roleName(request.getRole())
                .build();


        //User insert
        authRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {

       User user= authRepository.findByEmail(request.getEmail())
               .orElseThrow(()-> new LoginFailedException("존재하지 않는 이메일입니다."));

       if(!request.getPassword().equals(user.getPassword())){
           throw new LoginFailedException("비밀번호가 틀렸습니다.");
       }


       return user;







    }
}
