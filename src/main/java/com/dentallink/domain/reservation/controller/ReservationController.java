package com.dentallink.domain.reservation.controller;

import com.dentallink.common.response.ApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.dto.UpdateReservationStatusRequest;
import com.dentallink.domain.reservation.service.ReservationInternalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * TODO: 예약 생성은 HospitalSchedule 완성 후 추가 예정
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Validated
public class ReservationController {

    private final ReservationInternalService reservationService;

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

    //  예약 생성 (TODO: HospitalSchedule 완성 후 구현)

    //예약 승인
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservationStatus(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid UpdateReservationStatusRequest request,
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(1) Long userId) {

        reservationService.cancelReservation(id, userId);
        return ApiResponse.deleteSuccess("예약 취소 성공");
    }
}