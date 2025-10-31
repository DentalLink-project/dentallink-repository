package com.dentallink.domain.hospital.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record HospitalReservationResponse(
        Long hospitalId,
        Long userId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
) {}
