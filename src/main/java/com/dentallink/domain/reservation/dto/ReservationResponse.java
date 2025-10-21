package com.dentallink.domain.reservation.dto;

import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.enums.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long hospitalId,
        String hospitalName,
        Long userId,
        String username,
        LocalDateTime appointmentDate,
        ReservationStatus status,
        Long usedPoints,  // 사용한 포인트
        LocalDateTime createdAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getHospital().getId(),
                reservation.getHospital().getHospitalName(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getAppointmentDate(),
                reservation.getStatus(),
                reservation.getUsedPoints(),
                reservation.getCreatedAt()
        );
    }
}