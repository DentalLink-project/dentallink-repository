package com.dentallink.hospital.dto.response;

import lombok.Getter;

@Getter
public class HospitalCreateResponse {
    private final Long hospitalId;
    private final String hospitalName;

    public HospitalCreateResponse(Long hospitalId, String hospitalName) {
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
    }
}
