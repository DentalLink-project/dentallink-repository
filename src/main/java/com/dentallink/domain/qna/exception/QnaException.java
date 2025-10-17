package com.dentallink.domain.qna.exception;

import com.dentallink.common.exception.ErrorCode;
import com.dentallink.common.exception.GlobalException;

// QnA 도메인 전용 예외 정의
public class QnaException extends GlobalException {
    public QnaException(ErrorCode errorCode) {
        super(errorCode);
    }
}
