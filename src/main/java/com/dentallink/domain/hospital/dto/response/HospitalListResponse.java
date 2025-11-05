package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;

public record HospitalListResponse (
        Long id,
        String hospitalName,
        String doctorName,
        Boolean hospitalIsOpen,
        Boolean isFavorite
) {
    public static HospitalListResponse of(Hospital hospital, boolean isFavorite) {
        return new HospitalListResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getDoctorName(),
                hospital.getHospitalIsOpen(),
                isFavorite
        );
    }
}
