package com.example.lovequery.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum ErrorCode {


    EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST,"중복된 이메일입니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 이메일입니다."),
    INCORRECT_PASSWORD(HttpStatus.BAD_REQUEST,"비밀번호를 틀렸습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 유저입니다."),
    PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND,"플레이어 권한이 없습니다."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 루트입니다."),
    EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 에피소드입니다."),
    FAIL_NEXT_EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND,"다음 FAIL 에피소드가 존재하지 않습니다."),
    PASS_NEXT_EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND,"다음 PASS 에피소드가 존재하지 않습니다."),
    CHOICE_LOCKED(HttpStatus.FORBIDDEN,"호감도가 부족하여 선택할 수 없습니다."),
    BAD_EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 BAD 에피소드입니다."),
    TRUE_EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 TRUE 에피소드입니다."),
    NORMAL_EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 NORMAL 에피소드입니다."),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND,"존재하지 않는 캐릭터입니다."),
    FAIL_EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND,"다음 실패 에피소드가 존재하지 않습니다."),
    PASS_EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND,"다음 성공 에피소드가 존재하지 않습니다."),
    CHOICE_NOT_FOUND(HttpStatus.NOT_FOUND,"선택을 하지 않았습니다."),
    ADMIN_REQUEST_NO(HttpStatus.BAD_REQUEST,"관리자 역할은 신청할 수 없습니다."),
    NO_PERMISSION(HttpStatus.BAD_REQUEST,"허가되지 않은 권한입니다."),
    ROLE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND,"요청이 없습니다."),
    INVALID_ROLE_REQUEST(HttpStatus.BAD_REQUEST,"PENDING 상태의 요청이 아닙니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

}
