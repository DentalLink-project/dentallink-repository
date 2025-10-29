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
        Integer reservationCost,
        LocalTime openTime,
        LocalTime closeTime,
        LocalTime breakStart,
        LocalTime breakEnd
) {
    private HospitalDetailResponse ( // 생정자 별도 생성
            Long id,
            String hospitalName,
            String hospitalDescription,
            String hospitalAddress,
            Boolean isOpen,
            String doctorName,
            Integer reservationCost
    ) {
        this(id, hospitalName, hospitalDescription, hospitalAddress, isOpen, doctorName, reservationCost,
                null, null, null, null) ;
    }

    public static HospitalDetailResponse of(Hospital hospital, HospitalSchedule schedule) {
        if(schedule == null) { // (삭제되어서) 일정이 없는 병원을 조회할 시
            return new HospitalDetailResponse(
                    hospital.getId(),
                    hospital.getHospitalName(),
                    hospital.getHospitalDescription(),
                    hospital.getHospitalAddress(),
                    hospital.getHospitalIsOpen(),
                    hospital.getDoctorName(),
                    hospital.getReservationCost()
            );
        }

        return new HospitalDetailResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getHospitalDescription(),
                hospital.getHospitalAddress(),
                hospital.getHospitalIsOpen(),
                hospital.getDoctorName(),
                hospital.getReservationCost(),
                schedule.getOpenTime(),
                schedule.getCloseTime(),
                schedule.getBreakStart(),
                schedule.getBreakEnd()
        );
    }
}
