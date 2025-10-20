package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

public record HospitalDetailResponse (
        Long id,
        String hospitalName,
        String hospitalDescription,
        String hospitalAddress,
        Boolean isOpen,
        String doctorName,
        LocalTime openTime,
        LocalTime closeTime,
        LocalTime breakStart,
        LocalTime breakEnd
) {
    public static HospitalDetailResponse of(Hospital hospital, HospitalSchedule schedule) {
        return new HospitalDetailResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getHospitalDescription(),
                hospital.getHospitalAddress(),
                hospital.getHospitalIsOpen(),
                hospital.getDoctorName(),
                schedule.getOpenTime(),
                schedule.getCloseTime(),
                schedule.getBreakStart(),
                schedule.getBreakEnd()
        );
    }
}
