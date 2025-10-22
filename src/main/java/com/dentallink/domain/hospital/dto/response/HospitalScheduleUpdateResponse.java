package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.HospitalSchedule;

import java.time.LocalTime;

public record HospitalScheduleUpdateResponse(
        Long scheduleId,
        LocalTime openTime,
        LocalTime closeTime,
        LocalTime breakStart,
        LocalTime breakEnd
) {
    public static HospitalScheduleUpdateResponse of(HospitalSchedule schedule) {
        return new HospitalScheduleUpdateResponse(
                schedule.getId(),
                schedule.getOpenTime(),
                schedule.getCloseTime(),
                schedule.getBreakStart(),
                schedule.getBreakEnd()
        );
    }
}
