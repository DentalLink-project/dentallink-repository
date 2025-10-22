package com.dentallink.domain.hospital.controller;

import com.dentallink.domain.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalScheduleCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.service.HospitalExternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals")
public class HospitalController {
    private final HospitalExternalService hospitalExternalService;


    @PostMapping
    public ResponseEntity<HospitalCreateResponse> createHospital(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalCreateRequest request
    ) {
        HospitalCreateResponse response = hospitalExternalService.createHospital(authUser.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{hospitalId}/schedules")
    public ResponseEntity<HospitalScheduleCreateResponse> createHospitalSchedule(
            @PathVariable Long hospitalId,
            @Valid @RequestBody HospitalScheduleCreateRequest request
    ) {
        HospitalScheduleCreateResponse response =
                hospitalExternalService.createHospitalSchedule(hospitalId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<HospitalListResponse>> getAllHospitals(Pageable pageable) {
        Page<HospitalListResponse> hospitals = hospitalExternalService.findAllHospitals(pageable);
        return ResponseEntity.ok(hospitals);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HospitalDetailResponse> getHospitalById(@PathVariable Long id) {
        HospitalDetailResponse hospital = hospitalExternalService.findHospitalById(id);
        return ResponseEntity.ok(hospital);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HospitalUpdateResponse> updateHospital(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalUpdateRequest hospitalUpdateRequest
    ) {
        HospitalUpdateResponse hospital =
                hospitalExternalService.updateHospital(id, authUser.getUserId(), hospitalUpdateRequest);
        return ResponseEntity.ok(hospital);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHospital(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        hospitalExternalService.deleteHospital(id, authUser.getUserId());
        return ResponseEntity.noContent().build();
    }
}
