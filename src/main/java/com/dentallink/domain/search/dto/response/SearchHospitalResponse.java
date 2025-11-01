package com.dentallink.domain.search.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record SearchHospitalResponse(
    Long id,
    String hospitalName,
    String hospitalDescription,
    String hospitalAddress,
    LocalTime openTime,
    LocalTime closeTime,
    LocalTime breakStart,
    LocalTime breakEnd,
    LocalDateTime createdAt
) {
    public static SearchHospitalResponse from(Hospital hospital, HospitalSchedule hospitalSchedule) {
        return new SearchHospitalResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getHospitalDescription(),
                hospital.getHospitalAddress(),
                hospitalSchedule.getOpenTime(),
                hospitalSchedule.getCloseTime(),
                hospitalSchedule.getBreakStart(),
                hospitalSchedule.getBreakEnd(),
                hospital.getCreatedAt()
        );
    }
}
