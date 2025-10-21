package com.dentallink.domain.hospital.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalScheduleCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.exception.HospitalErrorCode;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    @Transactional
    public Hospital getHospitalById(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_NOT_FOUND));
    }

    // 병원 등록
    @Transactional
    public HospitalCreateResponse createHospital(HospitalCreateRequest hospitalCreateRequest) {
       Hospital hospital = new Hospital(
               hospitalCreateRequest.userId(),
               hospitalCreateRequest.hospitalName(),
               hospitalCreateRequest.hospitalDescription(),
               hospitalCreateRequest.hospitalAddress(),
               hospitalCreateRequest.hospitalIsOpen(),
               hospitalCreateRequest.doctorName()
       );

       Hospital createHospital = hospitalRepository.save(hospital);

       HospitalSchedule hospitalSchedule = new HospitalSchedule(
               hospitalCreateRequest.openTime(),
               hospitalCreateRequest.closeTime(),
               hospitalCreateRequest.breakStart(),
               hospitalCreateRequest.breakEnd(),
               createHospital
       );

       HospitalSchedule createHospitalSchedule = hospitalScheduleRepository.save(hospitalSchedule);

       return HospitalCreateResponse.of(createHospital, createHospitalSchedule);
    }

    // 병원 일정 등록
    @Transactional
    public HospitalScheduleCreateResponse createHospitalSchedule(HospitalScheduleCreateRequest hospitalScheduleCreateRequest) {
        Hospital hospital = getHospitalById(hospitalScheduleCreateRequest.hospitalId());

        HospitalSchedule hospitalSchedule = new HospitalSchedule(
                hospitalScheduleCreateRequest.openTime(),
                hospitalScheduleCreateRequest.closeTime(),
                hospitalScheduleCreateRequest.breakStart(),
                hospitalScheduleCreateRequest.breakEnd(),
                hospital
        );

        HospitalSchedule savedSchedule = hospitalScheduleRepository.save(hospitalSchedule);

        return HospitalScheduleCreateResponse.of(savedSchedule);
    }

    // 병원 전체 조회
    @Transactional(readOnly = true)
    public Page<HospitalListResponse> findAllHospitals(Pageable pageable) {
        return hospitalRepository.findAll(pageable)
                .map(HospitalListResponse::from);
    }

    // 병원 단건 조회
    @Transactional(readOnly = true)
    public HospitalDetailResponse findHospitalById(Long id) {
        Hospital hospital = getHospitalById(id);

        HospitalSchedule schedule = hospitalScheduleRepository.findByHospitalId(id)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));

        return HospitalDetailResponse.of(hospital, schedule);
    }

    // 병원 정보 수정
    @Transactional
    public HospitalUpdateResponse updateHospital(Long id, HospitalUpdateRequest hospitalUpdateRequest) {
        Hospital hospital = getHospitalById(id);

        hospital.updateHospital(
                hospitalUpdateRequest.hospitalName(),
                hospitalUpdateRequest.hospitalDescription(),
                hospitalUpdateRequest.hospitalAddress(),
                hospitalUpdateRequest.hospitalIsOpen(),
                hospitalUpdateRequest.doctorName()
        );

        HospitalSchedule schedule = hospitalScheduleRepository.findByHospitalId(id)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));

        schedule.updateSchedule(
                hospitalUpdateRequest.openTime(),
                hospitalUpdateRequest.closeTime(),
                hospitalUpdateRequest.breakStart(),
                hospitalUpdateRequest.breakEnd()
        );

        return HospitalUpdateResponse.of(hospital, schedule);
    }

    // 병원 삭제
    @Transactional
    public void deleteHospital(Long id) {
        Hospital hospital = getHospitalById(id);

        hospitalScheduleRepository.deleteByHospital(hospital);
        hospitalRepository.delete(hospital);
    }
}
