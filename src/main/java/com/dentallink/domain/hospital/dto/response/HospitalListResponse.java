package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HospitalListResponse {
    private Long id;
    private String hospitalName;
    private String doctorName;
    private Boolean isOpen;

    public static HospitalListResponse from(Hospital hospital) {
        return new HospitalListResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getDoctorName(),
                hospital.getHospitalIsOpen()
        );
    }
}
