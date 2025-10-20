package com.dentallink.domain.user.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum  UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    BAD_USER_ROLE(HttpStatus.BAD_REQUEST, "유효하지 않는 사용자 role 입니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
    USER_BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 입력입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
