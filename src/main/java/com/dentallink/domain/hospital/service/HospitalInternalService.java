package com.dentallink.domain.hospital.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.exception.HospitalErrorCode;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HospitalInternalService {
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    @Transactional(readOnly = true)
    public Hospital getHospitalById(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public HospitalSchedule getScheduleByHospitalId(Long hospitalId) {
        return hospitalScheduleRepository.findByHospitalId(hospitalId)
                .orElseThrow(() -> new GlobalException(HospitalErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<Hospital> findAllHospitals(Pageable pageable) {
        return hospitalRepository.findAll(pageable);
    }

    @Transactional
    public Hospital saveHospital(Hospital hospital) {
        return hospitalRepository.save(hospital);
    }

    @Transactional
    public HospitalSchedule saveHospitalSchedule(HospitalSchedule schedule) {
        return hospitalScheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteHospitalAndSchedule(Hospital hospital) {
        hospitalScheduleRepository.deleteByHospital(hospital);
        hospitalRepository.delete(hospital);
    }
}
