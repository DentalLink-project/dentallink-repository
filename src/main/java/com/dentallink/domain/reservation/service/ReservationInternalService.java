package com.dentallink.domain.reservation.service;


import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.dto.UpdateReservationStatusRequest;
import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.enums.ReservationStatus;
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

    //TODO: 예약생성은 병원도메인 작업 후 진행

    public Reservation getReservation(Long id) {
        return reservationRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    public Page<Reservation> getReservationsByUserId(Long userId, Pageable pageable) {
        return reservationRepository.findByUserId(userId, pageable);
    }

    public Page<Reservation> getReservationsByHospitalId(Long hospitalId, Pageable pageable) {
        return reservationRepository.findByHospitalId(hospitalId, pageable);
    }

    @Transactional
    public Reservation updateStatus(Reservation reservation, ReservationStatus status) {
        switch (status) {
            case APPROVED -> reservation.approve();
            case REJECTED -> reservation.reject();
            case COMPLETED -> reservation.complete();
            default -> throw new GlobalException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
        }
        return reservation;
    }

    @Transactional
    public void cancelReservation(Reservation reservation) {
        reservation.cancel();
    }
}