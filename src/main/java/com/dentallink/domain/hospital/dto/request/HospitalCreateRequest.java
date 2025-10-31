package com.dentallink.domain.hospital.dto.request;

import java.time.LocalTime;

public record HospitalCreateRequest (
    String hospitalName,
    String hospitalDescription,
    String hospitalAddress,
    Boolean hospitalIsOpen,
    String doctorName,
    Long reservationCost,
    LocalTime openTime,
    LocalTime closeTime,
    LocalTime breakStart,
    LocalTime breakEnd
) {}
