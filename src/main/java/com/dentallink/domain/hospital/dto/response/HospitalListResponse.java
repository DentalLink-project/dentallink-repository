package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HospitalListResponse {
    private final Long id;
    private final String hospitalName;
    private final String doctorName;
    private final Boolean isOpen;

    public static HospitalListResponse from(Hospital hospital) {
        return new HospitalListResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getDoctorName(),
                hospital.getHospitalIsOpen()
        );
    }
}
