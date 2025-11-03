package com.dentallink.domain.hospital.service;

import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class HospitalExternalService {

    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    public Page<Hospital> getHospitalsByKeyword(Pageable pageable, String keyword) {
        return hospitalRepository.findAllByHospitalNameContainingAndDeletedIsFalse(pageable, keyword);
    }
}
