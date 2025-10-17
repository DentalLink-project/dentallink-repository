package com.dentallink.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateReservationRequest(

        @NotNull(message = "병원 ID는 필수입니다")
        Long hospitalId,

        @NotNull(message = "예약 시간은 필수입니다")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime appointmentDate
) {
    public static CreateReservationRequest of(Long hospitalId, LocalDateTime appointmentDate) {
        return new CreateReservationRequest(hospitalId, appointmentDate);
    }
}