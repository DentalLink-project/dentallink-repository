package com.dentallink.domain.reservation.service;


import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.reservation.dto.*;
import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.execption.ReservationErrorCode;
import com.dentallink.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReservationInternalService {

    private final ReservationRepository reservationRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalScheduleRepository hospitalScheduleRepository;

    private static final int MAX_RESERVATIONS_PER_SLOT = 3;
    private static final int TIME_PERIOD = 30;


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

        Page<Reservation> reservations = reservationRepository.findByHospitalIdWithPaging(hospitalId, pageable);
        return reservations.map(ReservationResponse::from);
    }

    //예약 상태 변경 (병원 관리자)
    @Transactional
    public ReservationResponse updateReservationStatus(
            Long id,
            ReservationUpdateStatusRequest request,
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

    //예약 생성
    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request, Long userId) {

        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_NOT_FOUND));

        validateHospitalIsOpen(hospital);

        validateAppointmentDateTime(request.appointmentDate());

        HospitalSchedule schedule = hospitalScheduleRepository.findByHospitalId(request.hospitalId())
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));

        validateBusinessHours(request.appointmentDate(), schedule);

        validateTimeSlotAvailability(request.hospitalId(), request.appointmentDate());

        validateDuplicateUserReservation(userId, request.appointmentDate());

        Reservation reservation = Reservation.create(
                request.hospitalId(),
                userId,
                request.appointmentDate()
        );

        Reservation saveReservation = reservationRepository.save(reservation);
        return ReservationResponse.from(saveReservation);

    }

    //예약 가능한 시간대 조회
    public List<AvailableTimeSlotResponse> getAvailableTimePeriod(Long hospitalId, LocalDate date) {

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_NOT_FOUND));

        validateHospitalIsOpen(hospital);

        HospitalSchedule schedule = hospitalScheduleRepository.findByHospitalId(hospitalId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));

        List<LocalDateTime> timesPeriod = generateTimePeriod(date, schedule);

        LocalDateTime startDay = date.atStartOfDay();
        LocalDateTime endDay = date.atTime(LocalTime.MAX);

        // DB에서 GROUP BY로 시간대별 예약 개수 조회
        List<ReservationCountDto> reservationCounts = reservationRepository.countReservationsByTimeSlot(
                hospitalId, startDay, endDay
        );

        // Map으로 변환
        Map<LocalDateTime, Long> reservationCountMap = reservationCounts.stream()
                .collect(Collectors.toMap(
                        ReservationCountDto::getTimeSlot,
                        ReservationCountDto::getCount
                ));

        List<AvailableTimeSlotResponse> availableTimeSlots = new ArrayList<>();

        for (LocalDateTime timeSlot : timesPeriod) {
            long existingCount = reservationCountMap.getOrDefault(timeSlot, 0L);
            int availableCount = MAX_RESERVATION_PER_MAN - (int) existingCount;

            AvailableTimeSlotResponse response = AvailableTimeSlotResponse.of(
                    timeSlot,
                    Math.max(0, availableCount)
            );

            availableTimeSlots.add(response);
        }

        return availableTimeSlots;
    }

    private void validateHospitalIsOpen(Hospital hospital) {
        if (!hospital.getHospitalIsOpen()) {
            throw new GlobalException(ReservationErrorCode.HOSPITAL_CLOSED);
        }
    }

    private void validateAppointmentDateTime(LocalDateTime appointmentDate) {
        if (appointmentDate.isBefore(LocalDateTime.now())) {
            throw new GlobalException(ReservationErrorCode.PAST_APPOINTMENT_TIME);
        }

        if (appointmentDate.getMinute() % TIME_PERIOD != 0 || appointmentDate.getSecond() != 0) {
            throw new GlobalException(ReservationErrorCode.INVALID_TIME_UNIT);
        }
    }

    private void validateBusinessHours(LocalDateTime appointmentDate, HospitalSchedule schedule) {
        LocalTime appointmentTime = appointmentDate.toLocalTime();

        if (appointmentTime.isBefore(schedule.getOpenTime()) ||
                appointmentTime.isAfter(schedule.getCloseTime())) {
            throw new GlobalException(ReservationErrorCode.OUTSIDE_BUSINESS_HOURS);
        }

        if (schedule.getBreakStart() != null && schedule.getBreakEnd() != null) {
            if (!appointmentTime.isBefore(schedule.getBreakStart()) &&
                    appointmentTime.isBefore(schedule.getBreakEnd())) {
                throw new GlobalException(ReservationErrorCode.BREAK_TIME);
            }

        }
    }

    private void validateTimeSlotAvailability(Long hospitalId, LocalDateTime appointmentDate) {
        int currentReservationCount = reservationRepository.countByHospitalIdAndAppointmentDate(
                hospitalId, appointmentDate
        );

        if (currentReservationCount >= MAX_RESERVATION_PER_MAN) {
            throw new GlobalException(ReservationErrorCode.RESERVATION_FULL);
        }
    }

    private void validateDuplicateUserReservation(Long userId, LocalDateTime appointmentDate) {
        boolean exists = reservationRepository.existsByUserIdAndAppointmentDate(
                userId, appointmentDate
        );

        if (exists) {
            throw new GlobalException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }
    }

    private List<LocalDateTime> generateTimePeriod(LocalDate date, HospitalSchedule schedule) {
        List<LocalDateTime> timePeriod = new ArrayList<>();
        LocalTime currentTime = schedule.getOpenTime();

        while (currentTime.isBefore(schedule.getCloseTime())) {
            // 점심시간이 아닌 경우에만 추가
            if (schedule.getBreakStart() == null || schedule.getBreakEnd() == null ||
                    currentTime.isBefore(schedule.getBreakStart()) ||
                    !currentTime.isBefore(schedule.getBreakEnd())) {

                // 과거 시간이 아닌 경우에만 추가
                LocalDateTime timeSlot = LocalDateTime.of(date, currentTime);
                if (!timeSlot.isBefore(LocalDateTime.now())) {
                    timePeriod.add(timeSlot);
                }
            }

            currentTime = currentTime.plusMinutes(TIME_PERIOD);
        }

        return timePeriod;
    }

    private void validateHospitalAdmin(Long hospitalId, Long userId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_NOT_FOUND));

        if (!hospital.getUserId().equals(userId)) {
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