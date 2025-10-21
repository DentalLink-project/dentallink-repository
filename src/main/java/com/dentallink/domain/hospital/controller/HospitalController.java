package com.dentallink.domain.hospital.controller;

import com.dentallink.domain.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalScheduleCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hospital")
public class HospitalController {
    private final HospitalService hospitalService;

    @PostMapping
    public ResponseEntity<HospitalCreateResponse> createHospital(
            @RequestBody HospitalCreateRequest hospitalCreateRequest
    ){
        return ResponseEntity.ok(hospitalService.createHospital(hospitalCreateRequest));
    }

    @PostMapping("/{hospitalId}/schedules")
    public ResponseEntity<HospitalScheduleCreateResponse> createHospitalSchedule(
            @PathVariable Long hospitalId,
            @RequestBody HospitalScheduleCreateRequest request
    ) {
        HospitalScheduleCreateResponse response = hospitalService.createHospitalSchedule(hospitalId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<HospitalListResponse>> getAllHospitals(Pageable pageable) {
        Page<HospitalListResponse> hospitals = hospitalService.findAllHospitals(pageable);
        return ResponseEntity.ok(hospitals);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HospitalDetailResponse> getHospitalById(@PathVariable Long id) {
        HospitalDetailResponse hospital = hospitalService.findHospitalById(id);
        return ResponseEntity.ok(hospital);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HospitalUpdateResponse> updateHospital(
            @PathVariable Long id,
            @RequestBody HospitalUpdateRequest hospitalUpdateRequest
    ){
        HospitalUpdateResponse hospital = hospitalService.updateHospital(id, hospitalUpdateRequest);
        return ResponseEntity.ok(hospital);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHospital(@PathVariable Long id) {
        hospitalService.deleteHospital(id);
        return ResponseEntity.noContent().build();
    }
}
