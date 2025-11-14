package com.dentallink.domain.reservation.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.reservation.dto.AvailableTimeSlotResponse;
import com.dentallink.domain.reservation.dto.ReservationCreateRequest;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.dto.ReservationUpdateStatusRequest;
import com.dentallink.domain.reservation.service.ReservationInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "예약 관리", description = "치과 예약 관련 API")
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Validated
public class ReservationController {

    private final ReservationInternalService reservationService;

    @Operation(summary = "예약 생성", description = "새로운 치과 예약을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "예약 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<ReservationResponse>> createReservation(
            @Parameter(description = "예약 생성 정보")
            @RequestBody @Valid ReservationCreateRequest request,
            @AuthenticationPrincipal AuthUser authUser) {

        ReservationResponse response = reservationService.createReservation(request, authUser.getUserId());
        return CommonApiResponse.created(response, "예약 생성 성공");
    }

    @Operation(summary = "예약 가능 시간 조회",
            description = "특정 병원의 특정 날짜에 예약 가능한 시간을 조회합니다. 인증 불필요")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping("/available-slots")
    public ResponseEntity<CommonApiResponse<List<AvailableTimeSlotResponse>>> getAvailableTimePeriod(
            @Parameter(description = "병원 ID", required = true)
            @RequestParam @Min(1) Long hospitalId,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AvailableTimeSlotResponse> availableSlots = reservationService.getAvailableTimePeriod(hospitalId, date);
        return CommonApiResponse.success(availableSlots, "예약 가능 시간 조회 성공");
    }

    @Operation(summary = "예약 단건 조회",
            description = "예약 ID로 특정 예약 정보를 조회합니다. 본인, 병원 관리자, 시스템 관리자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음")
    })
    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<ReservationResponse>> getReservation(
            @Parameter(description = "예약 ID")
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser) {

        ReservationResponse response = reservationService.getReservation(id, authUser.getUserId());
        return CommonApiResponse.success(response, "예약 조회 성공");
    }

    @Operation(summary = "내 예약 목록 조회",
            description = "로그인한 사용자의 예약 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<PageResponse<ReservationResponse>>> getMyReservations(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 정보 (page, size, sort)")
            @PageableDefault(size = 10, sort = "appointmentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ReservationResponse> reservations = reservationService.getMyReservations(authUser.getUserId(), pageable);
        return CommonApiResponse.pageSuccess(reservations, "내 예약 목록 조회 성공");
    }

    @Operation(summary = "병원별 예약 목록 조회",
            description = "특정 병원의 예약 목록을 조회합니다. 병원 관리자, 시스템 관리자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<PageResponse<ReservationResponse>>> getHospitalReservations(
            @Parameter(description = "병원 ID", required = true)
            @RequestParam Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 정보 (page, size, sort)")
            @PageableDefault(size = 10, sort = "appointmentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ReservationResponse> reservations = reservationService.getHospitalReservations(
                hospitalId,
                authUser.getUserId(),
                pageable
        );
        return CommonApiResponse.pageSuccess(reservations, "병원 예약 목록 조회 성공");
    }

    @Operation(summary = "예약 상태 변경",
            description = "예약 상태를 변경합니다. 병원 관리자, 시스템 관리자만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<CommonApiResponse<ReservationResponse>> updateReservationStatus(
            @Parameter(description = "예약 ID")
            @PathVariable @Min(1) Long id,
            @Parameter(description = "변경할 상태 정보")
            @RequestBody @Valid ReservationUpdateStatusRequest request,
            @AuthenticationPrincipal AuthUser authUser) {

        ReservationResponse response = reservationService.updateReservationStatus(
                id,
                request,
                authUser.getUserId()
        );
        return CommonApiResponse.success(response, "예약 상태 변경 성공");
    }

    @Operation(summary = "예약 취소",
            description = "예약을 취소합니다. 본인만 가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "예약을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonApiResponse<Void>> cancelReservation(
            @Parameter(description = "예약 ID")
            @PathVariable @Min(1) Long id,
            @AuthenticationPrincipal AuthUser authUser) {

        reservationService.cancelReservation(id, authUser.getUserId());
        return CommonApiResponse.deleteSuccess("예약 취소 성공");
    }
}