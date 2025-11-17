package com.example.lovequery.common.exception;

public record ErrorResponse(
        String code,
        String message,
        int status
) {
}
