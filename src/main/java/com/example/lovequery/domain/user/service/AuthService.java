package com.example.lovequery.domain.user.service;


import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.user.entity.User;
import com.example.lovequery.domain.user.dto.LoginRequest;
import com.example.lovequery.domain.user.dto.SignUpRequest;
import com.example.lovequery.domain.user.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public void signUp(SignUpRequest request) {
        if (authRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATE);
        }

        String encodedPassword =passwordEncoder.encode(request.password());

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(encodedPassword)
                .roleName(request.role())
                .build();


        //User insert
        authRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {

       User user= authRepository.findByEmail(request.email())
               .orElseThrow(()-> new CustomException(ErrorCode.EMAIL_NOT_FOUND));

       if(!passwordEncoder.matches(request.password(),user.getPassword())){
           throw new CustomException(ErrorCode.INCORRECT_PASSWORD);
       }


       return user;







    }
}
