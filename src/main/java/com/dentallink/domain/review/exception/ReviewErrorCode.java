package com.dentallink.domain.review.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리뷰는 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
