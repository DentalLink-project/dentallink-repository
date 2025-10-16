package com.dentallink.domain.reservation.service;



import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.dto.UpdateReservationStatusRequest;
import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.execption.ReservationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReservationExternalService {

    private final ReservationInternalService internalService;

    public ReservationResponse getReservation(Long id) {
        Reservation reservation = internalService.getReservation(id);
        return ReservationResponse.from(reservation);
    }

    public Page<ReservationResponse> getMyReservations(Long userId, Pageable pageable) {
        Page<Reservation> reservations = internalService.getReservationsByUserId(userId, pageable);
        return reservations.map(ReservationResponse::from);
    }

    public Page<ReservationResponse> getHospitalReservations(
            Long hospitalId,
            Long hospitalAdminId,
            Pageable pageable) {

        validateHospitalAdmin(hospitalId, hospitalAdminId);
        Page<Reservation> reservations = internalService.getReservationsByHospitalId(hospitalId, pageable);
        return reservations.map(ReservationResponse::from);
    }

    /**
     * 예약 상태 변경 (병원 관리자)
     */
    @Transactional
    public ReservationResponse updateReservationStatus(
            Long id,
            UpdateReservationStatusRequest request,
            Long hospitalAdminId) {

        Reservation reservation = internalService.getReservation(id);
        validateHospitalAdmin(reservation.getHospitalId(), hospitalAdminId);
        Reservation updated = internalService.updateStatus(reservation, request.status());
        return ReservationResponse.from(updated);
    }

    /**
     * 예약 취소 (사용자)
     */
    @Transactional
    public void cancelReservation(Long id, Long userId) {
        Reservation reservation = internalService.getReservation(id);
        validateReservationOwner(reservation, userId);
        validateAppointmentTime(reservation);
        internalService.cancelReservation(reservation);
    }

    /**
     * 예약 취소 (시스템/다른 도메인용)
     */
    @Transactional
    public void cancelReservationBySystem(Long id) {
        Reservation reservation = internalService.getReservation(id);
        internalService.cancelReservation(reservation);
    }

    // ==================== 검증 로직 ====================

    private void validateHospitalAdmin(Long hospitalId, Long hospitalAdminId) {
        if (hospitalId == null || hospitalAdminId == null) {
            throw new GlobalException(ReservationErrorCode.NOT_HOSPITAL_ADMIN);
        }
    }

    private void validateReservationOwner(Reservation reservation, Long userId) {
        if (!reservation.getUserId().equals(userId)) {
            throw new GlobalException(ReservationErrorCode.NOT_RESERVATION_OWNER);
        }
    }

    private void validateAppointmentTime(Reservation reservation) {
        if (reservation.getAppointmentDate().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ReservationErrorCode.PAST_APPOINTMENT_TIME);
        }
    }
}