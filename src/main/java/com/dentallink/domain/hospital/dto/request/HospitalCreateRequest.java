package com.dentallink.domain.hospital.dto.request;

import java.time.LocalTime;

public record HospitalCreateRequest (
        Long userId,
    String hospitalName,
    String hospitalDescription,
    String hospitalAddress,
    Boolean hospitalIsOpen,
    String doctorName,
    LocalTime openTime,
    LocalTime closeTime,
    LocalTime breakStart,
    LocalTime breakEnd
) {}
