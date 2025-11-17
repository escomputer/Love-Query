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
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

}
