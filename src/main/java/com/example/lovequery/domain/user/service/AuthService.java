package com.example.lovequery.domain.user.service;


import com.example.lovequery.common.exception.CustomException;
import com.example.lovequery.common.exception.ErrorCode;
import com.example.lovequery.domain.player.entity.Player;
import com.example.lovequery.domain.player.repository.PlayerRepository;
import com.example.lovequery.domain.user.dto.LoginRequest;
import com.example.lovequery.domain.user.dto.LoginResponse;
import com.example.lovequery.domain.user.dto.SignUpRequest;
import com.example.lovequery.domain.user.entity.Role;
import com.example.lovequery.domain.user.entity.RoleRequest;
import com.example.lovequery.domain.user.entity.User;
import com.example.lovequery.domain.user.repository.AuthRepository;
import com.example.lovequery.domain.user.repository.RoleRequestRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlayerRepository playerRepository;
    private final RoleRequestRepository roleRequestRepository;


    @Transactional
    public void signUp(SignUpRequest request) {
        if (authRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_DUPLICATE);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(encodedPassword)
                .role(Role.PLAYER) //기본으로 player
                .build();


        //User insert
        authRepository.save(user);

        playerRepository.save(new Player(user));

        Role requestedRole = request.role();

        if (requestedRole == Role.PLAYER) {
            return;
        }

        if (requestedRole == Role.GAME_ADMIN) {
            throw new CustomException(ErrorCode.ADMIN_REQUEST_NO);
        }

        RoleRequest roleRequest = new RoleRequest(
                user,
                requestedRole
        );

        roleRequestRepository.save(roleRequest);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req, HttpSession session) {

        User user = authRepository.findByEmail(req.email())
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INCORRECT_PASSWORD);
        }

        // 세션에 유저 id 저장
        session.setAttribute("userId", user.getId());

        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getRole()
        );
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }


}

