package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;

public record HospitalListResponse (
        Long id,
        String hospitalName,
        String doctorName,
        Boolean hospitalIsOpen
) {
    public static HospitalListResponse from(Hospital hospital) {
        return new HospitalListResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getDoctorName(),
                hospital.getHospitalIsOpen()
        );
    }
}
