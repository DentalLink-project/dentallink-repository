package com.dentallink.domain.hospital.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HospitalErrorCode implements ErrorCode {
    NOT_HOSPITAL_OWNER(HttpStatus.FORBIDDEN, "병원 등록자만 수정할 수 있습니다."),
    NOT_RESERVATION_OWNER(HttpStatus.FORBIDDEN, "예약한 본인만 취소할 수 있습니다."),

    DUPLICATE_SCHEDULE(HttpStatus.CONFLICT, "이미 해당 병원에는 일정이 존재합니다."),
    ALREADY_RESERVED(HttpStatus.CONFLICT, "이미 해당 시간에는 예약이 존재합니다."),

    HOSPITAL_NOT_FOUND(HttpStatus.NOT_FOUND, "병원을 찾을 수 없습니다."),
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND,"취소할 예약을 찾을 수 없습니다."),
    HOSPITAL_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND,"병원 스케줄을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
