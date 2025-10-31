package com.dentallink.domain.chatbot.exception;


import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatbotErrorCode implements ErrorCode {
    // 세션 관련
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 종료된 세션입니다."),
    SESSION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "활성화되지 않은 세션입니다."),

    // 메시지 관련
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "메시지 내용이 비어있습니다."),
    MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지가 너무 깁니다. (최대 2000자)"),

    // API 관련
    GEMINI_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 서비스 오류가 발생했습니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."),

    // 상담원 관련
    NO_AVAILABLE_CONSULTANT(HttpStatus.SERVICE_UNAVAILABLE, "현재 대기 중인 상담원이 없습니다."),
    CONSULTANT_NOT_FOUND(HttpStatus.NOT_FOUND, "상담원을 찾을 수 없습니다."),
    NOT_CONSULTANT_SESSION(HttpStatus.BAD_REQUEST, "상담원 세션이 아닙니다."),

    // 권한 관련
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "세션에 접근할 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
