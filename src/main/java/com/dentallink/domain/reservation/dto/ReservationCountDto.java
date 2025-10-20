package com.dentallink.domain.reservation.dto;

import java.time.LocalDateTime;

public interface ReservationCountDto {
    LocalDateTime getTimeSlot();
    Long getCount();
}
