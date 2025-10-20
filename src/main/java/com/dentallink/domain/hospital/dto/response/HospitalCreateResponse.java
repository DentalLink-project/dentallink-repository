package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record HospitalCreateResponse (
        Long id,
        String hospitalName,
        String hospitalDescription,
        String hospitalAddress,
        Boolean isOpen,
        String doctorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalTime openTime,
        LocalTime closeTime,
        LocalTime breakStart,
        LocalTime breakEnd
) {
    public static HospitalCreateResponse of(Hospital hospital, HospitalSchedule  hospitalSchedule) {
        return new HospitalCreateResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getHospitalDescription(),
                hospital.getHospitalAddress(),
                hospital.getHospitalIsOpen(),
                hospital.getDoctorName(),
                hospital.getCreatedAt(),
                hospital.getUpdatedAt(),
                hospitalSchedule.getOpenTime(),
                hospitalSchedule.getCloseTime(),
                hospitalSchedule.getBreakStart(),
                hospitalSchedule.getBreakEnd()
        );
    }
}
