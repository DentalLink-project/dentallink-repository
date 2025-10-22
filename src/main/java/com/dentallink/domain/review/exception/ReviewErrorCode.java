package com.dentallink.domain.review.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),

    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 변경입니다"),

    NOT_REVIEW_OWNER(HttpStatus.FORBIDDEN, "작성자만 수정할 수 있습니다."),

    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 해당 시간에 예약이 존재합니다"),
    ALREADY_DELETED(HttpStatus.CONFLICT, "이미 삭제된 리뷰입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
