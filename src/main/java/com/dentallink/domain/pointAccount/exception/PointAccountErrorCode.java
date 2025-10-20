package com.dentallink.domain.pointAccount.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PointAccountErrorCode implements ErrorCode {

    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "포인트 계좌를 찾을 수 없습니다."),
    ACCOUNT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 포인트 계좌가 존재합니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
