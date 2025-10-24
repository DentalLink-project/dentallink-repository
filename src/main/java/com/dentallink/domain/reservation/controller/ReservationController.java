package com.dentallink.domain.reservation.controller;


import com.dentallink.common.response.ApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.reservation.dto.AvailableTimeSlotResponse;
import com.dentallink.domain.reservation.dto.ReservationCreateRequest;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.dto.ReservationUpdateStatusRequest;
import com.dentallink.domain.reservation.service.ReservationInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
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

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Validated
public class ReservationController {

    private final ReservationInternalService reservationService;

    //예약 생성
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @RequestBody @Valid ReservationCreateRequest request,
            @AuthenticationPrincipal AuthUser authUser) {

        ReservationResponse response = reservationService.createReservation(request, authUser.getUserId());
        return ApiResponse.created(response, "예약 생성 성공");
    }

    //예약 가능 시간 조회 (인증 불필요)
    @GetMapping("/available-slots")
    public ResponseEntity<ApiResponse<List<AvailableTimeSlotResponse>>> getAvailableTimePeriod(
            @RequestParam @Min(1) Long hospitalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AvailableTimeSlotResponse> availableSlots = reservationService.getAvailableTimePeriod(hospitalId, date);
        return ApiResponse.success(availableSlots, "예약 가능 시간 조회 성공");
    }

    //예약 단건 조회 (본인, 병원 관리자, 시스템 관리자만 가능)
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser) {

        ReservationResponse response = reservationService.getReservation(id, authUser.getUserId());
        return ApiResponse.success(response, "예약 조회 성공");
    }

    //내 예약 목록 조회
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> getMyReservations(
            @AuthenticationPrincipal AuthUser authUser,
            @PageableDefault(size = 10, sort = "appointmentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ReservationResponse> reservations = reservationService.getMyReservations(authUser.getUserId(), pageable);
        return ApiResponse.pageSuccess(reservations, "내 예약 목록 조회 성공");
    }

    //병원별 예약 목록 조회 (병원 관리자, 시스템 관리자만 가능)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> getHospitalReservations(
            @RequestParam Long hospitalId,
            @AuthenticationPrincipal AuthUser authUser,
            @PageableDefault(size = 10, sort = "appointmentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ReservationResponse> reservations = reservationService.getHospitalReservations(
                hospitalId,
                authUser.getUserId(),
                pageable
        );
        return ApiResponse.pageSuccess(reservations, "병원 예약 목록 조회 성공");
    }

    //예약 상태 변경 (병원 관리자, 시스템 관리자만 가능)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL')")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservationStatus(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid ReservationUpdateStatusRequest request,
            @AuthenticationPrincipal AuthUser authUser) {

        ReservationResponse response = reservationService.updateReservationStatus(
                id,
                request,
                authUser.getUserId()
        );
        return ApiResponse.success(response, "예약 상태 변경 성공");
    }

    //예약 취소 (본인만 가능)
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable @Min(1) Long id,
            @AuthenticationPrincipal AuthUser authUser) {

        reservationService.cancelReservation(id, authUser.getUserId());
        return ApiResponse.deleteSuccess("예약 취소 성공");
    }
}