package com.dentallink.domain.reservation.dto;

import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.enums.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long hospitalId,
        Long userId,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime appointmentDate,

        ReservationStatus status,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getHospitalId(),
                reservation.getUserId(),
                reservation.getAppointmentDate(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }
}