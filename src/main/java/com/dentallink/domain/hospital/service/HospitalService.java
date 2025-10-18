package com.dentallink.domain.hospital.service;

import com.dentallink.domain.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import com.dentallink.domain.hospital.dto.response.HospitalCreateResponse;
import com.dentallink.domain.hospital.dto.response.HospitalUpdateResponse;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    /**
     * 병원 등록
     *
     * @param hospitalCreateRequest 병원 생성 요청 DTO
     * @return 생성된 병원 및 스케줄 정보를 담은 응답 DTO
     */
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


    @Transactional
    public HospitalUpdateResponse updateHospital(Long id, HospitalUpdateRequest hospitalUpdateRequest){


        return  new HospitalUpdateResponse(

        );
    }
}
