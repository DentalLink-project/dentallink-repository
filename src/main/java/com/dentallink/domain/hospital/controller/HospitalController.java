package com.dentallink.domain.hospital.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.hospital.dto.request.*;
import com.dentallink.domain.hospital.dto.response.*;
import com.dentallink.domain.hospital.service.HospitalInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.dentallink.common.response.CommonApiResponse.*;

@Tag(name = "병원 관리", description = "병원과 병원 일정 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals")
public class HospitalController {
    private final HospitalInternalService hospitalInternalService;

    // 병원 전체 조회
    @Operation(summary = "병원 전체 조회", description = "등록된 모든 병원을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<CommonApiResponse<PageResponse<HospitalListResponse>>> getAllHospitals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        Long userId = (authUser != null) ? authUser.getUserId() : null;

        return success(
                hospitalInternalService.findAllHospitals(page, size, userId),
                "병원 목록을 조회했습니다."
        );
    }

    // 병원 단건 조회
    @Operation(summary = "병원 상세 조회", description = "특정 병원의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "병원을 찾을 수 없음")
    })
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<CommonApiResponse<HospitalDetailResponse>> getHospitalById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        Long userId = (authUser != null) ? authUser.getUserId() : null;

        HospitalDetailResponse hospital = hospitalInternalService.findHospitalById(id, userId);

        return success(hospital, "병원 상세 정보를 조회했습니다.");
    }

    // 병원 등록
    @Operation(summary = "병원 등록", description = "새로운 병원을 등록합니다. 시스템 관리자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "병원 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<CommonApiResponse<HospitalCreateResponse>> createHospital(
            @Valid @RequestBody HospitalCreateRequest request
    ) {
        return created(
                hospitalInternalService.createHospital(request),
                "병원이 성공적으로 등록되었습니다."
        );
    }

    // 병원 관계자 지정(등록)
    @Operation(summary = "병원 관계자 지정", description = "특정 유저를 병원 관계자로 지정합니다. 시스템 관리자 또는 병원 관계자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "병원 관계자 등록 성공")
    })
    @PatchMapping("/{hospitalId}/assign/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<String>> assignHospitalMember(
            @PathVariable Long hospitalId,
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return success(
                hospitalInternalService.assignHospitalMember(hospitalId, userId, authUser.getUserId()),
                "해당 사용자가 병원 관계자로 등록되었습니다."
        );
    }

    // 병원 수정
    @Operation(summary = "병원 수정", description = "병원 정보를 수정합니다. 병원 관리자, 시스템 관리자 또는 병원 관계자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "병원을 찾을 수 없음")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<HospitalUpdateResponse>> updateHospital(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalUpdateRequest hospitalUpdateRequest
    ) {
        HospitalUpdateResponse hospital =
                hospitalInternalService.updateHospital(id, authUser.getUserId(), hospitalUpdateRequest);
        return success(
                hospital, "병원이 성공적으로 수정되었습니다."
        );
    }

    // 병원 삭제
    @Operation(summary = "병원 삭제", description = "병원을 삭제합니다. 시스템 관리자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "병원을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<CommonApiResponse<Void>> deleteHospital(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        hospitalInternalService.deleteHospital(id, authUser.getUserId());
        return deleteSuccess(
                "병원이 성공적으로 삭제되었습니다."
        );
    }

    // 병원 일정 등록
    @Operation(summary = "병원 일정 등록", description = "병원의 근무 일정을 등록합니다. 병원 관리자, 시스템 관리자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "병원 일정 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "병원을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 일정이 존재함")
    })
    @PostMapping("/{hospitalId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<HospitalScheduleCreateResponse>> createHospitalSchedule(
            @PathVariable Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalScheduleCreateRequest request
    ) {
        HospitalScheduleCreateResponse response =
                hospitalInternalService.createHospitalSchedule(hospitalId, authUser.getUserId(), request);
        return created(
                response,
                "병원 일정이 성공적으로 등록되었습니다."
        );
    }

    // 병원 일정 수정
    @Operation(summary = "병원 일정 수정", description = "병원의 근무 일정을 수정합니다. 병원 관리자, 시스템 관리자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "병원 혹은 일정을 찾을 수 없음")
    })
    @PatchMapping("/{hospitalId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<HospitalScheduleUpdateResponse>> updateHospitalSchedule(
            @PathVariable Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalScheduleUpdateRequest request
    ) {
        HospitalScheduleUpdateResponse response = hospitalInternalService.updateHospitalSchedule(hospitalId, authUser.getUserId(), request);
        return success(
                response,
                "병원 일정이 성공적으로 수정되었습니다."
        );
    }

    // 병원 일정 삭제
    @Operation(summary = "병원 일정 삭제", description = "병원의 근무 일정을 삭제합니다. 병원 관리자, 시스템 관리자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "병원 혹은 일정을 찾을 수 없음")
    })
    @DeleteMapping("/{hospitalId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<Void>> deleteHospitalSchedule(
            @PathVariable Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        hospitalInternalService.deleteHospitalSchedule(hospitalId, authUser.getUserId());
        return deleteSuccess(
                "병원 일정이 성공적으로 삭제되었습니다."
        );
    }
}
