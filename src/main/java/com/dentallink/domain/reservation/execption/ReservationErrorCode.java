package com.dentallink.domain.reservation.execption;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다"),

    INVALID_APPOINTMENT_DATE(HttpStatus.BAD_REQUEST, "유효하지 않은 예약 시간입니다"),
    PAST_APPOINTMENT_TIME(HttpStatus.BAD_REQUEST, "과거 시간으로 예약할 수 없습니다"),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 변경입니다"),

    NOT_RESERVATION_OWNER(HttpStatus.FORBIDDEN, "예약 소유자만 접근할 수 있습니다"),
    NOT_HOSPITAL_ADMIN(HttpStatus.FORBIDDEN, "병원 관리자만 접근할 수 있습니다"),

    RESERVATION_TIME_NOT_AVAILABLE(HttpStatus.CONFLICT, "예약 불가능한 시간입니다"),
    ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 예약입니다"),
    ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 예약입니다");

    private final HttpStatus httpStatus;
    private final String message;
}
