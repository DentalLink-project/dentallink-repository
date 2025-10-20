package com.dentallink.domain.review.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),

    NOT_REVIEW_OWNER(HttpStatus.FORBIDDEN, "작성자만 수정할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
