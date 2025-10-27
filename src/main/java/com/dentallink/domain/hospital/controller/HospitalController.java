package com.dentallink.domain.hospital.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.hospital.dto.request.HospitalCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalScheduleCreateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalScheduleUpdateRequest;
import com.dentallink.domain.hospital.dto.request.HospitalUpdateRequest;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.service.HospitalExternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.CommonApiResponse.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals")
public class HospitalController {
    private final HospitalExternalService hospitalExternalService;

    // 병원 등록
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<CommonApiResponse<HospitalCreateResponse>> createHospital(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalCreateRequest request
    ) {
        return created(
                hospitalExternalService.createHospital(authUser.getUserId(), request),
                "병원이 성공적으로 등록되었습니다."
        );
    }

    // 병원 전체 조회
    @GetMapping
    public ResponseEntity<CommonApiResponse<PageResponse<HospitalListResponse>>> getAllHospitals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return success (
                hospitalExternalService.findAllHospitals(page, size),
                "병원 목록을 조회했습니다."
        );
    }

    // 병원 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<CommonApiResponse<HospitalDetailResponse>> getHospitalById(@PathVariable Long id) {
        HospitalDetailResponse hospital = hospitalExternalService.findHospitalById(id);
        return success(
                hospital, "병원 상세 정보를 조회했습니다."
        );
    }

    // 병원 수정
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<HospitalUpdateResponse>> updateHospital(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalUpdateRequest hospitalUpdateRequest
    ) {
        HospitalUpdateResponse hospital =
                hospitalExternalService.updateHospital(id, authUser.getUserId(), hospitalUpdateRequest);
        return success(
                hospital, "병원이 성공적으로 수정되었습니다."
        );
    }

    // 병원 삭제
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<CommonApiResponse<Void>> deleteHospital(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        hospitalExternalService.deleteHospital(id, authUser.getUserId());
        return deleteSuccess(
                "병원이 성공적으로 삭제되었습니다."
        );
    }

    // 병원 일정 등록
    @PostMapping("/{hospitalId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<HospitalScheduleCreateResponse>> createHospitalSchedule(
            @PathVariable Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalScheduleCreateRequest request
    ) {
        HospitalScheduleCreateResponse response =
                hospitalExternalService.createHospitalSchedule(hospitalId, authUser.getUserId(), request);
        return created(
                response,
                "병원 일정이 성공적으로 등록되었습니다."
        );
    }

    // 병원 일정 수정
    @PatchMapping("/{hospitalId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<HospitalScheduleUpdateResponse>> updateHospitalSchedule(
            @PathVariable Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalScheduleUpdateRequest request
    ) {
        HospitalScheduleUpdateResponse response = hospitalExternalService.updateHospitalSchedule(hospitalId, authUser.getUserId(), request);
        return success(
                response,
                "병원 일정이 성공적으로 수정되었습니다."
        );
    }

    // 병원 일정 삭제
    @DeleteMapping("/{hospitalId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<Void>> deleteHospitalSchedule(
            @PathVariable Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        hospitalExternalService.deleteHospitalSchedule(hospitalId, authUser.getUserId());
        return deleteSuccess(
                "병원 일정이 성공적으로 삭제되었습니다."
        );
    }
}
