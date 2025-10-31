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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.dentallink.common.response.CommonApiResponse.*;

@Tag(name = "병원 관리", description = "병원과 병원 일정 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals")
public class HospitalController {
    private final HospitalInternalService hospitalInternalService;

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
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalCreateRequest request
    ) {
        return created(
                hospitalInternalService.createHospital(authUser.getUserId(), request),
                "병원이 성공적으로 등록되었습니다."
        );
    }

    // 병원 전체 조회
    @Operation(summary = "병원 전체 조회", description = "등록된 모든 병원을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<CommonApiResponse<PageResponse<HospitalListResponse>>> getAllHospitals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return success (
                hospitalInternalService.findAllHospitals(page, size),
                "병원 목록을 조회했습니다."
        );
    }

    // 병원 단건 조회
    @Operation(summary = "병원 상세 조회", description = "특정 병원의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "병원을 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CommonApiResponse<HospitalDetailResponse>> getHospitalById(@PathVariable Long id) {
        HospitalDetailResponse hospital = hospitalInternalService.findHospitalById(id);
        return success(
                hospital, "병원 상세 정보를 조회했습니다."
        );
    }

    // 병원 수정
    @Operation(summary = "병원 수정", description = "병원 정보를 수정합니다. 병원 관리자, 시스템 관리자만 가능")
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

    // 병원 예약 가능 시간 자동 생성 테스트용
    @PostMapping("/{hospitalId}/available-times")
    public ResponseEntity<String> createAvailableTimesManually(
            @PathVariable Long hospitalId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        hospitalInternalService.hospitalAvailableTimesTest(hospitalId, date);
        return ResponseEntity.ok("병원: " + hospitalId + " / 날짜: " + date);
    }

    // 병원 예약 가능 시간 조회
    @Operation(summary = "병원 예약 가능 시간 조회", description = "사용자가 특정 병원의 예약 가능한 시간을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "병원을 찾을 수 없음")
    })
    @GetMapping("/{hospitalId}/available-times")
    public ResponseEntity<CommonApiResponse<List<HospitalReservationTimeResponse>>> getAvailableTimes(
            @PathVariable Long hospitalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<HospitalReservationTimeResponse> response = hospitalInternalService.getAvailableTimes(hospitalId, date);
        return success(
                response,
                "예약 가능 시간이 조회되었습니다."
        );
    }

    // 병원 예약 등록
    @Operation(summary = "병원 예약 등록", description = "병원 예약을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "병원 예약 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "병원 혹은 예약 가능 시간을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 예약된 시간")
    })
    @PostMapping("/{hospitalId}/reservations")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CommonApiResponse<HospitalReservationResponse>> createHospitalReservation(
            @PathVariable Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody HospitalReservationCreateRequest request
    ) {
        HospitalReservationResponse response = hospitalInternalService.createHospitalReservation(
                hospitalId,
                authUser.getUserId(),
                request
        );
        return created(
                response,
                "병원 예약이 성공적으로 등록되었습니다."
        );
    }

    // 병원 예약 취소
    @Operation(summary = "병원 예약 취소", description = "자신의 병원 예약을 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "예약 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음")
    })
    @DeleteMapping("/{hospitalId}/reservations/{reservationId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CommonApiResponse<Void>> cancelHospitalReservation(
            @PathVariable Long hospitalId,
            @PathVariable Long reservationId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        hospitalInternalService.cancelHospitalReservation(hospitalId, reservationId, authUser.getUserId());
        return deleteSuccess(
                "병원 예약이 성공적으로 취소되었습니다."
        );
    }
}
