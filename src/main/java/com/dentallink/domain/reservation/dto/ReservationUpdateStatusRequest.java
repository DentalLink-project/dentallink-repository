package com.dentallink.domain.reservation.dto;

import com.dentallink.domain.reservation.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record ReservationUpdateStatusRequest(

        @NotNull(message = "변경할 상태는 필수입니다.")
        ReservationStatus status
){

    public static ReservationUpdateStatusRequest of(ReservationStatus status) {
        return new ReservationUpdateStatusRequest(status);
    }
}
