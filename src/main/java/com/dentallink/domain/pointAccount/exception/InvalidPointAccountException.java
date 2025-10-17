package com.dentallink.domain.pointAccount.exception;

import com.dentallink.common.exception.ErrorCode;
import com.dentallink.common.exception.GlobalException;

public class InvalidPointAccountException extends GlobalException {
    public InvalidPointAccountException(ErrorCode errorCode) {
        super(errorCode);
    }
}
