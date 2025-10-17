package com.dentallink.domain.qna.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// QnA 도메인 전용 에러 코드 정의
@Getter
@RequiredArgsConstructor
public enum QnaErrorCode implements ErrorCode {

    // ====== Question 관련 ======
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "문의글을 찾을 수 없습니다."),
    QUESTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인 문의글만 수정하거나 삭제할 수 있습니다."),

    // ====== Answer 관련 ======
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "답변글을 찾을 수 없습니다."),
    ANSWER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인 답변글만 수정하거나 삭제할 수 있습니다."),

    // ====== Validation ======
    INVALID_QNA_INPUT(HttpStatus.BAD_REQUEST, "잘못된 QnA 요청입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
