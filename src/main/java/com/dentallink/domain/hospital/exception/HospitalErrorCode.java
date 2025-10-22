package com.dentallink.domain.hospital.exception;

import com.dentallink.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HospitalErrorCode implements ErrorCode {

    HOSPITAL_NOT_FOUND(HttpStatus.NOT_FOUND, "병원을 찾을 수 없습니다."),

    NOT_HOSPITAL_OWNER(HttpStatus.FORBIDDEN, "병원 등록자만 수정할 수 있습니다."),

    HOSPITAL_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND,"병원 스케줄을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
