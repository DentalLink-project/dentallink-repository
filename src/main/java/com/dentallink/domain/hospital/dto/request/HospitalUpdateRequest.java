package com.dentallink.domain.hospital.dto.request;

import lombok.Getter;

import java.time.LocalTime;

@Getter
public class HospitalUpdateRequest {
    private String hospitalName;
    private String hospitalDescription;
    private String hospitalAddress;
    private Boolean hospitalIsOpen;
    private String doctorName;
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalTime breakStart;
    private LocalTime breakEnd;
}
