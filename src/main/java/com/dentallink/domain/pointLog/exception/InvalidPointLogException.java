package com.dentallink.domain.pointLog.exception;

import com.dentallink.common.exception.ErrorCode;
import com.dentallink.common.exception.GlobalException;

public class InvalidPointLogException extends GlobalException {
    public InvalidPointLogException(ErrorCode errorCode) {
        super(errorCode);
    }
}
