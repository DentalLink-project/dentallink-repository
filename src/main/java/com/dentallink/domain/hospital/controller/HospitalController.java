package com.dentallink.domain.hospital.controller;

import com.dentallink.domain.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import com.dentallink.domain.hospital.dto.response.HospitalCreateResponse;
import com.dentallink.domain.hospital.dto.response.HospitalUpdateResponse;
import com.dentallink.domain.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class HospitalController {
    private final HospitalService hospitalService;

    @PostMapping("/hospital")
    public ResponseEntity<HospitalCreateResponse> createHospital(
            @RequestBody HospitalCreateRequest hospitalCreateRequest
    ){
        return ResponseEntity.ok(hospitalService.createHospital(hospitalCreateRequest));
    }

    @PatchMapping("hospital/{id}")
    public ResponseEntity<HospitalUpdateResponse> updateHospital(
            @PathVariable Long id,
            @RequestBody HospitalUpdateRequest hospitalUpdateRequest
    ){
        return ResponseEntity.ok(hospitalService.updateHospital(id, hospitalUpdateRequest));
    }
}
