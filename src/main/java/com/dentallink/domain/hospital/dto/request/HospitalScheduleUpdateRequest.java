package com.dentallink.domain.hospital.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record HospitalScheduleUpdateRequest(
        @NotNull LocalTime openTime,
        @NotNull LocalTime closeTime,
        @NotNull LocalTime breakStart,
        @NotNull LocalTime breakEnd
) {}
