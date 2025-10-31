package com.dentallink.domain.hospital.dto.response;

import java.time.LocalTime;

public record HospitalReservationTimeResponse(
        Long id,
        LocalTime startTime,
        LocalTime endTime
) {
    public static HospitalReservationTimeResponse of(Long id, LocalTime startTime, LocalTime endTime) {
        return new HospitalReservationTimeResponse(id, startTime, endTime);
    }
}
