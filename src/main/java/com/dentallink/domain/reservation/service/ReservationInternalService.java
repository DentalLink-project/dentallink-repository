package com.dentallink.domain.reservation.service;


import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.dto.UpdateReservationStatusRequest;
import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.execption.ReservationErrorCode;
import com.dentallink.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReservationInternalService {

    private final ReservationRepository reservationRepository;

    //예약 조회 (단건)
    public ReservationResponse getReservation(Long id) {
        Reservation reservation = reservationRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        return ReservationResponse.from(reservation);
    }


    //내 예약 목록 조회
    public Page<ReservationResponse> getMyReservations(Long userId, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByUserId(userId, pageable);
        return reservations.map(ReservationResponse::from);
    }

    /**
     * 병원의 예약 목록 조회 (병원 관리자)
     */
    public Page<ReservationResponse> getHospitalReservations(
            Long hospitalId,
            Long hospitalAdminId,
            Pageable pageable) {

        // 병원 관리자 권한 확인
        validateHospitalAdmin(hospitalId, hospitalAdminId);

        Page<Reservation> reservations = reservationRepository.findByHospitalId(hospitalId, pageable);
        return reservations.map(ReservationResponse::from);
    }

    //예약 상태 변경 (병원 관리자)
    @Transactional
    public ReservationResponse updateReservationStatus(
            Long id,
            UpdateReservationStatusRequest request,
            Long hospitalAdminId) {

        Reservation reservation = reservationRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 병원 관리자 권한 확인
        validateHospitalAdmin(reservation.getHospitalId(), hospitalAdminId);

        // 상태 변경
        switch (request.status()) {
            case APPROVED -> reservation.approve();
            case REJECTED -> reservation.reject();
            case COMPLETED -> reservation.complete();
            default -> throw new GlobalException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
        }

        return ReservationResponse.from(reservation);
    }

    //예약 취소
    @Transactional
    public void cancelReservation(Long id, Long userId) {
        Reservation reservation = reservationRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 예약 소유자 확인
        validateReservationOwner(reservation, userId);

        // 예약 시간 확인 (과거 예약 취소 불가)
        validateAppointmentTime(reservation);

        // 취소 처리
        reservation.cancel();
    }


    // TODO: Hospital Entity 생성 후 실제 권한 확인 로직 추가

    private void validateHospitalAdmin(Long hospitalId, Long hospitalAdminId) {
        // TODO: Hospital 도메인 완성 후 구현
        // Hospital hospital = hospitalRepository.findById(hospitalId);
        // if (!hospital.getUserId().equals(hospitalAdminId)) {
        //     throw new GlobalException(ReservationErrorCode.NOT_HOSPITAL_ADMIN);
        // }

        // 임시: hospitalId와 hospitalAdminId가 유효한지만 체크
        if (hospitalId == null || hospitalAdminId == null) {
            throw new GlobalException(ReservationErrorCode.NOT_HOSPITAL_ADMIN);
        }
    }

    //소유자 확인

    private void validateReservationOwner(Reservation reservation, Long userId) {
        if (!reservation.getUserId().equals(userId)) {
            throw new GlobalException(ReservationErrorCode.NOT_RESERVATION_OWNER);
        }
    }

    //시간
    private void validateAppointmentTime(Reservation reservation) {
        if (reservation.getAppointmentDate().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ReservationErrorCode.PAST_APPOINTMENT_TIME);
        }
    }
}