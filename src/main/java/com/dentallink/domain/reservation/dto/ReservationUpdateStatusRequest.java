package com.dentallink.domain.reservation.dto;

import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import org.hibernate.sql.Update;

public record UpdateReservationStatusRequest (

        @NotNull(message = "변경할 상태는 필수입니다.")
        ReservationStatus status
){

    public static UpdateReservationStatusRequest of(ReservationStatus status) {
        return new UpdateReservationStatusRequest(status);
    }
}
