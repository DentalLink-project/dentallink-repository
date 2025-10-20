package com.dentallink.domain.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;


//예약 가능한 시간대
public record AvailableTimeSlotResponse(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime timeSlot,

        int availableCount,

        boolean isAvailable
) {
    public static AvailableTimeSlotResponse of(LocalDateTime timeSlot, int availableCount) {
        return new AvailableTimeSlotResponse(
                timeSlot,
                availableCount,
                availableCount > 0
        );
    }

}
