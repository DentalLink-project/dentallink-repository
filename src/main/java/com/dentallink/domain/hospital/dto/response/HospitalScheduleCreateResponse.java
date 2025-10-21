package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.HospitalSchedule;

import java.time.LocalTime;

public record HospitalScheduleCreateResponse (
        Long id,
        Long hospitalId,
        LocalTime openTime,
        LocalTime closeTime,
        LocalTime breakStart,
        LocalTime breakEnd
) {
    public static HospitalScheduleCreateResponse of(HospitalSchedule schedule) {
        return new HospitalScheduleCreateResponse(
                schedule.getId(),
                schedule.getHospital().getId(),
                schedule.getOpenTime(),
                schedule.getCloseTime(),
                schedule.getBreakStart(),
                schedule.getBreakEnd()
        );
    }
}
