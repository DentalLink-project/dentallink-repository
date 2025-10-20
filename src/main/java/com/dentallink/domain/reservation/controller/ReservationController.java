package com.dentallink.domain.reservation.controller;


import com.dentallink.common.response.ApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.reservation.dto.AvailableTimeSlotResponse;
import com.dentallink.domain.reservation.dto.ReservationCreateRequest;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.dto.ReservationUpdateStatusRequest;
import com.dentallink.domain.reservation.service.ReservationInternalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @RequestBody @Valid ReservationCreateRequest request,
            @RequestParam @Min(1) Long userId) {

        ReservationResponse response = reservationService.createReservation(request, userId);
        return ApiResponse.success(response, "예약 성공");
    }

    //예약 가능 시간 조회
    @GetMapping("/available-slots")
    public ResponseEntity<ApiResponse<List<AvailableTimeSlotResponse>>> getAvailableTimePeriod(
            @RequestParam @Min(1) Long hospitalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<AvailableTimeSlotResponse> availableSlots = reservationService.getAvailableTimePeriod(hospitalId, date);
        return ApiResponse.success(availableSlots, "예약 가능 시간 조회 성공");
    }

    //예약 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservation(
            @PathVariable Long id) {

        ReservationResponse response = reservationService.getReservation(id);
        return ApiResponse.success(response, "예약 조회 성공");
    }

    //내 예약 목록 조회
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> getMyReservations(
            @RequestParam @Min(1) Long userId,
            @PageableDefault(size = 10, sort = "appointmentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ReservationResponse> reservations = reservationService.getMyReservations(userId, pageable);
        return ApiResponse.pageSuccess(reservations, "내 예약 목록 조회 성공");
    }

    //병원별 예약 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> getHospitalReservations(
            @RequestParam Long hospitalId,
            @RequestParam Long hospitalAdminId,
            @PageableDefault(size = 10, sort = "appointmentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ReservationResponse> reservations = reservationService.getHospitalReservations(
                hospitalId,
                hospitalAdminId,
                pageable
        );
        return ApiResponse.pageSuccess(reservations, "병원 예약 목록 조회 성공");
    }

    //예약 상태 변경 (병원 관리자)
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservationStatus(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid ReservationUpdateStatusRequest request,
            @RequestParam @Min(1) Long hospitalAdminId) {

        ReservationResponse response = reservationService.updateReservationStatus(
                id,
                request,
                hospitalAdminId
        );
        return ApiResponse.success(response, "예약 상태 변경 성공");
    }

    //예약 취소
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(1) Long userId) {

        reservationService.cancelReservation(id, userId);
        return ApiResponse.deleteSuccess("예약 취소 성공");
    }
}