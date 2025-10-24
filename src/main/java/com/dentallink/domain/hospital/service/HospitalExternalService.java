package com.dentallink.domain.hospital.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalScheduleCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalScheduleUpdateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.exception.HospitalErrorCode;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HospitalExternalService {
    private final HospitalInternalService hospitalInternalService;
    private final HospitalRepository hospitalRepository;

    // 병원 등록
    @Transactional
    public HospitalCreateResponse createHospital(Long userId, HospitalCreateRequest req) {
        Hospital hospital = new Hospital(
                userId,
                req.hospitalName(),
                req.hospitalDescription(),
                req.hospitalAddress(),
                req.hospitalIsOpen(),
                req.doctorName()
        );

        Hospital savedHospital = hospitalInternalService.saveHospital(hospital);

        HospitalSchedule schedule = new HospitalSchedule(
                req.openTime(),
                req.closeTime(),
                req.breakStart(),
                req.breakEnd(),
                savedHospital
        );
        HospitalSchedule savedSchedule = hospitalInternalService.saveHospitalSchedule(schedule);

        return HospitalCreateResponse.of(savedHospital, savedSchedule);
    }

    // 병원 전체 조회
    @Transactional(readOnly = true)
    public Page<HospitalListResponse> findAllHospitals(Pageable pageable) {
        return hospitalInternalService.findAllHospitals(pageable)
                .map(HospitalListResponse::from);
    }

    // 병원 단건 조회
    @Transactional(readOnly = true)
    public HospitalDetailResponse findHospitalById(Long id) {
        Hospital hospital = hospitalInternalService.getHospitalWithScheduleById(id);
        return HospitalDetailResponse.of(hospital, hospital.getHospitalSchedule());
    }

    // 병원 수정
    @Transactional
    public HospitalUpdateResponse updateHospital(Long id, Long userId, HospitalUpdateRequest req) {
        Hospital hospital = hospitalInternalService.getHospitalById(id);

        checkHospitalOwner(hospital, userId);

        hospital.updateHospital(
                req.hospitalName(),
                req.hospitalDescription(),
                req.hospitalAddress(),
                req.hospitalIsOpen(),
                req.doctorName()
        );

        HospitalSchedule schedule = hospitalInternalService.getScheduleByHospitalId(id);
        schedule.updateSchedule(
                req.openTime(),
                req.closeTime(),
                req.breakStart(),
                req.breakEnd()
        );

        return HospitalUpdateResponse.of(hospital, schedule);
    }

    // 병원 삭제
    @Transactional
    public void deleteHospital(Long id, Long userId) {
        Hospital hospital = hospitalInternalService.getHospitalById(id);

        checkHospitalOwner(hospital, userId);

        hospitalInternalService.deleteHospitalAndSchedule(hospital);
    }

    // 병원 일정 등록
    @Transactional
    public HospitalScheduleCreateResponse createHospitalSchedule(Long hospitalId, Long userId, HospitalScheduleCreateRequest req) {
        Hospital hospital = hospitalInternalService.getHospitalById(hospitalId);

        checkHospitalOwner(hospital, userId);

        hospitalInternalService.findScheduleByHospitalId(hospitalId)
                .ifPresent(existing -> {
                    throw new GlobalException(HospitalErrorCode.DUPLICATE_SCHEDULE);
                });

        HospitalSchedule schedule = new HospitalSchedule(
                req.openTime(),
                req.closeTime(),
                req.breakStart(),
                req.breakEnd(),
                hospital
        );

        HospitalSchedule savedSchedule = hospitalInternalService.saveHospitalSchedule(schedule);
        return HospitalScheduleCreateResponse.of(savedSchedule);
    }

    // 병원 일정 수정
    @Transactional
    public HospitalScheduleUpdateResponse updateHospitalSchedule(Long hospitalId, Long userId, HospitalScheduleUpdateRequest req) {
        Hospital hospital = hospitalInternalService.getHospitalById(hospitalId);

        checkHospitalOwner(hospital, userId);

        HospitalSchedule schedule = hospitalInternalService.getScheduleByHospitalId(hospitalId);

        schedule.updateSchedule(
                req.openTime(),
                req.closeTime(),
                req.breakStart(),
                req.breakEnd()
        );

        return HospitalScheduleUpdateResponse.of(schedule);
    }

    // 병원 일정 삭제
    @Transactional
    public void deleteHospitalSchedule(Long hospitalId, Long userId) {
        Hospital hospital = hospitalInternalService.getHospitalById(hospitalId);
        checkHospitalOwner(hospital, userId);

        HospitalSchedule schedule = hospitalInternalService.getScheduleByHospitalId(hospitalId);
        hospitalInternalService.deleteHospitalSchedule(schedule);
        hospital.deleteHospitalSchedule(); // 일정 삭제 시 병원 테이블에서도 일정을 삭제시킴
    }

    // 병원 소유권 확인
    private void checkHospitalOwner(Hospital hospital, Long userId) {
        if (!hospital.getUserId().equals(userId)) {
            throw new GlobalException(HospitalErrorCode.NOT_HOSPITAL_OWNER);
        }
    }
}
