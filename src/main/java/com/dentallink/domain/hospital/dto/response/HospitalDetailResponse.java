package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class HospitalDetailResponse {
    private Long id;
    private String hospitalName;
    private String hospitalDescription;
    private String hospitalAddress;
    private Boolean hospitalIsOpen;
    private String doctorName;
    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalTime breakStart;
    private LocalTime breakEnd;

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
