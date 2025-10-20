package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HospitalUpdateResponse {
    private final Long id;
    private final String hospitalName;
    private final String hospitalDescription;
    private final String hospitalAddress;
    private final Boolean hospitalIsOpen;
    private final String doctorName;
    private final LocalTime openTime;
    private final LocalTime closeTime;
    private final LocalTime breakStart;
    private final LocalTime breakEnd;

    public static HospitalUpdateResponse of(Hospital hospital, HospitalSchedule schedule) {
        return new HospitalUpdateResponse(
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
