package com.dentallink.domain.hospital.dto.response;

import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import lombok.Getter;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
public class HospitalCreateResponse {
    private final Long id;
    private final String hospitalName;
    private final String hospitalDescription;
    private final String hospitalAddress;
    private final Boolean isOpen;
    private final String doctorName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalTime openTime;
    private final LocalTime closeTime;
    private final LocalTime breakStart;
    private final LocalTime breakEnd;

    public HospitalCreateResponse(
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
        this.id = id;
        this.hospitalName = hospitalName;
        this.hospitalDescription = hospitalDescription;
        this.hospitalAddress = hospitalAddress;
        this.isOpen = isOpen;
        this.doctorName = doctorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.breakStart = breakStart;
        this.breakEnd = breakEnd;
    }

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
