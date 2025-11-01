package com.dentallink.domain.auth.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효한 Refresh Token이 없습니다."),
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "로그인 정보가 일치하지 않습니다."),
    FORBIDDEN_USER(HttpStatus.FORBIDDEN, "권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
