package com.dentallink.domain.pointLog.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PointLogErrorCode implements ErrorCode {

    ;
    private final HttpStatus httpStatus;
    private final String message;
}