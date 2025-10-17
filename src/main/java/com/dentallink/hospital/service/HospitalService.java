package com.dentallink.hospital.service;

import com.dentallink.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.hospital.dto.response.HospitalCreateResponse;
import com.dentallink.hospital.entity.Hospital;
import com.dentallink.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitalRepository;

    @Transactional
    public HospitalCreateResponse createHospital(HospitalCreateRequest hospitalCreateRequest)
    {
       Hospital hospital = new Hospital(
               hospitalCreateRequest.getHospitalName()
       );

       Hospital createHospital = hospitalRepository.save(hospital);

       return new HospitalCreateResponse(
               createHospital.getId(),
               createHospital.getHospitalName()
       );
    }
}
