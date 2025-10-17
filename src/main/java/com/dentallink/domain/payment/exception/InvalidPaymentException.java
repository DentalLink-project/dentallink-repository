package com.dentallink.domain.payment.exception;

import com.dentallink.common.exception.ErrorCode;
import com.dentallink.common.exception.GlobalException;

public class InvalidPaymentException extends GlobalException {
    public InvalidPaymentException(ErrorCode errorCode) {
        super(errorCode);
    }
}
