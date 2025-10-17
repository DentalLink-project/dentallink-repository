package com.dentallink.hospital.service;

import com.dentallink.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.hospital.dto.response.HospitalCreateResponse;
import com.dentallink.hospital.entity.Hospital;
import com.dentallink.hospital.entity.HospitalSchedule;
import com.dentallink.hospital.repository.HospitalRepository;
import com.dentallink.hospital.repository.HospitalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    @Transactional
    public HospitalCreateResponse createHospital(HospitalCreateRequest hospitalCreateRequest)
    {
       Hospital hospital = new Hospital(
               hospitalCreateRequest.getHospitalName(),
               hospitalCreateRequest.getHospitalDescription(),
               hospitalCreateRequest.getHospitalAddress(),
               hospitalCreateRequest.getHospitalIsOpen(),
               hospitalCreateRequest.getHospitalImage()
       );

       Hospital createHospital = hospitalRepository.save(hospital);

       HospitalSchedule hospitalSchedule = new HospitalSchedule(
               hospitalCreateRequest.getOpenTime(),
               hospitalCreateRequest.getCloseTime(),
               hospitalCreateRequest.getBreakStart(),
               hospitalCreateRequest.getBreakEnd(),
               hospital
       );

       HospitalSchedule createHospitalSchedule = hospitalScheduleRepository.save(hospitalSchedule);

       return new HospitalCreateResponse(
               createHospital.getId(),
               createHospital.getHospitalName(),
               createHospital.getHospitalDescription(),
               createHospital.getHospitalAddress(),
               createHospital.getHospitalIsOpen(),
               createHospital.getHospitalImage(),
               createHospital.getCreatedAt(),
               createHospital.getUpdatedAt(),
               createHospitalSchedule.getOpenTime(),
               createHospitalSchedule.getCloseTime(),
               createHospitalSchedule.getBreakStart(),
               createHospitalSchedule.getBreakEnd()
       );
    }


}
