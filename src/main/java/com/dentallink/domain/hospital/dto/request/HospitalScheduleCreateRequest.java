package com.dentallink.domain.hospital.dto.request;

import java.time.LocalTime;

public record HospitalScheduleCreateRequest (
        Long hospitalId,
        LocalTime openTime,
        LocalTime closeTime,
        LocalTime breakStart,
        LocalTime breakEnd
) {
}
