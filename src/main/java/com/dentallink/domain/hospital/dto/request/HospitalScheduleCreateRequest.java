package com.dentallink.domain.hospital.dto.request;

import java.time.LocalTime;

public record HospitalScheduleCreateRequest (
        LocalTime openTime,
        LocalTime closeTime,
        LocalTime breakStart,
        LocalTime breakEnd
) {
}
