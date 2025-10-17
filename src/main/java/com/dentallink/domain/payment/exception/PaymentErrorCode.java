package com.dentallink.domain.payment.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    ;
    private final HttpStatus httpStatus;
    private final String message;
}