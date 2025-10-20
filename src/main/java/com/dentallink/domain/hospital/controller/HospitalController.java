package com.dentallink.domain.hospital.controller;

import com.dentallink.domain.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import com.dentallink.domain.hospital.dto.response.HospitalCreateResponse;
import com.dentallink.domain.hospital.dto.response.HospitalDetailResponse;
import com.dentallink.domain.hospital.dto.response.HospitalListResponse;
import com.dentallink.domain.hospital.dto.response.HospitalUpdateResponse;
import com.dentallink.domain.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<HospitalListResponse>> getAllHospitals() {
        List<HospitalListResponse> hospitals = hospitalService.findAllHospitals();
        return ResponseEntity.ok(hospitals);
    }

    @GetMapping
    public ResponseEntity<HospitalDetailResponse> getHospitalById(@PathVariable Long id) {
        HospitalDetailResponse hospital = hospitalService.findHospitalById(id);
        return ResponseEntity.ok(hospital);
    }

    @PatchMapping("hospital/{id}")
    public ResponseEntity<HospitalUpdateResponse> updateHospital(
            @PathVariable Long id,
            @RequestBody HospitalUpdateRequest hospitalUpdateRequest
    ){
        HospitalUpdateResponse hospital = hospitalService.updateHospital(id, hospitalUpdateRequest);
        return ResponseEntity.ok(hospitalService.updateHospital(id, hospitalUpdateRequest));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteHospital(@PathVariable Long id) {
        hospitalService.deleteHospital(id);
        return ResponseEntity.noContent().build();
    }
}
