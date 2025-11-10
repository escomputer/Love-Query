package com.example.lovequery.repository;

import com.example.lovequery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<User,Long> {

    //로그인시 이메일로 사용자 조회(이메일은 중복이 안되니깐)
    Optional<User> findByName(String name);

    //이메일 중복 여부 확인
    boolean existsByEmail(String name);

    Optional<User> findByEmail(String email);
}
