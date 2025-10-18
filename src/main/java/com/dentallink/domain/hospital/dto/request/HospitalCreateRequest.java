package com.dentallink.domain.hospital.dto.request;

import lombok.Getter;

import java.time.LocalTime;

@Getter
public class HospitalCreateRequest {
    private String hospitalName;
    private String hospitalDescription;
    private String hospitalAddress;
    private Boolean hospitalIsOpen;
    private String hospitalImage;
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalTime breakStart;
    private LocalTime breakEnd;
}
