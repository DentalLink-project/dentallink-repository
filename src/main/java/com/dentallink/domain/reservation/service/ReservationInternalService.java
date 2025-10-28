package com.dentallink.domain.reservation.service;


import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.reservation.dto.AvailableTimeSlotResponse;
import com.dentallink.domain.reservation.dto.ReservationCountDto;
import com.dentallink.domain.reservation.dto.ReservationCreateRequest;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.dto.ReservationUpdateStatusRequest;
import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.execption.ReservationErrorCode;
import com.dentallink.domain.reservation.repository.ReservationRepository;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final PointAccountExternalService pointAccountExternalService;

    private static final int MAX_RESERVATIONS_PER_TIME_SLOT = 3;
    private static final int TIME_PERIOD = 30;

    // TODO: 향후 개선 - Payment 도메인과 연동하여 실제 결제 금액 기반으로 포인트 차감
    // TODO: 또는 Hospital 엔티티에 consultationFee 필드 추가하여 병원별 진료비 관리
    // 현재는 테스트용으로 1000 포인트 고정
    private static final Long RESERVATION_COST_POINTS = 1000L;


    //예약 조회 (단건) - 권한 체크 포함
    public ReservationResponse getReservation(Long id, Long userId) {
        Reservation reservation = reservationRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 권한 체크: 본인 예약 or 병원 관리자 or 시스템 관리자
        validateReservationAccess(reservation, userId);

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

        // 병원 소유권 확인 (@PreAuthorize로 역할은 체크됨)
        validateHospitalOwnership(hospitalId, hospitalAdminId);

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

        // 병원 소유권 확인 (@PreAuthorize로 역할은 체크됨)
        validateHospitalOwnership(reservation.getHospital().getId(), hospitalAdminId);

        // 상태 변경
        switch (request.status()) {
            case APPROVED -> reservation.approve();
            case REJECTED -> {
                // 환불 가능 포인트 계산
                Long refundablePoints = reservation.getRefundablePoints();

                // 예약 거절
                reservation.reject();

                // 포인트 환불 (환불 가능한 경우만)
                if (refundablePoints > 0) {
                    PointAccount pointAccount = pointAccountExternalService.getPointAccountByUser(reservation.getUser());
                    pointAccountExternalService.refundPointAccount(pointAccount.getId(), refundablePoints);
                }
            }
            case COMPLETED -> reservation.complete();
            default -> throw new GlobalException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
        }

        return ReservationResponse.from(reservation);
    }

    /**
     * 예약 취소 - 포인트 환불 포함
     */
    @Transactional
    public void cancelReservation(Long id, Long userId) {
        Reservation reservation = reservationRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 예약 소유자 확인
        validateReservationOwner(reservation, userId);

        // 예약 시간 확인 (과거 예약 취소 불가)
        validateAppointmentTime(reservation);

        // 환불할 포인트 계산 (완료된 예약은 환불 불가)
        Long refundablePoints = reservation.getRefundablePoints();

        // 예약 취소
        reservation.cancel();

        // 포인트 환불 (환불 가능한 경우만)
        if (refundablePoints > 0) {
            PointAccount pointAccount = pointAccountExternalService.getPointAccountByUser(reservation.getUser());
            pointAccountExternalService.refundPointAccount(pointAccount.getId(), refundablePoints);
        }
    }

    /**
     * 예약 생성 - 포인트 차감 포함
     */
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

        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.USER_NOT_FOUND));

        try {
            PointAccount pointAccount = pointAccountExternalService.getPointAccountByUser(user);
            pointAccountExternalService.spendPointAccount(pointAccount.getId(), RESERVATION_COST_POINTS);
        } catch (IllegalStateException e) {
            // PointAccount.spend()에서 발생하는 "잔액이 부족합니다" 예외를 비즈니스 예외로 변환
            throw new GlobalException(ReservationErrorCode.INSUFFICIENT_POINTS);
        }

        Reservation reservation = Reservation.create(
                hospital,
                user,
                request.appointmentDate(),
                RESERVATION_COST_POINTS
        );

        Reservation savedReservation = reservationRepository.save(reservation);

        return ReservationResponse.from(savedReservation);
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
            int availableCount = MAX_RESERVATIONS_PER_TIME_SLOT - (int) existingCount;

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

        // 마감 시간은 초과하면 안 됨 (closeTime 이상이면 예외)
        if (appointmentTime.isBefore(schedule.getOpenTime()) ||
                !appointmentTime.isBefore(schedule.getCloseTime())) {
            throw new GlobalException(ReservationErrorCode.OUTSIDE_BUSINESS_HOURS);
        }

        // 휴게시간 체크
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

        if (currentReservationCount >= MAX_RESERVATIONS_PER_TIME_SLOT) {
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

        // 마감 시간 30분 전까지만 예약 가능
        LocalTime lastSlot = schedule.getCloseTime().minusMinutes(TIME_PERIOD);

        while (!currentTime.isAfter(lastSlot)) {
            // 휴게시간이 아닌 경우에만 추가
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

    /**
     * 병원 소유권 검증
     * @PreAuthorize로 역할은 이미 체크되었으므로, 비즈니스 로직만 체크
     */
    private void validateHospitalOwnership(Long hospitalId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.USER_NOT_FOUND));

        // ADMIN은 모든 병원 접근 가능 (이미 @PreAuthorize에서 체크됨)
        if (user.getUserRole() == UserRole.ROLE_ADMIN) {
            return;
        }

        // HOSPITAL_OWNER는 자기 병원만 접근 가능 (비즈니스 로직)
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_NOT_FOUND));

        if (!hospital.getUserId().equals(userId)) {
            throw new GlobalException(ReservationErrorCode.NOT_HOSPITAL_ADMIN);
        }
    }

    /**
     * 예약 조회 권한 검증
     * - 시스템 관리자: 모든 예약 조회 가능
     * - 예약 소유자: 본인 예약 조회 가능
     * - 병원 관리자: 자기 병원 예약 조회 가능
     */
    private void validateReservationAccess(Reservation reservation, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.USER_NOT_FOUND));

        // 1. 시스템 관리자는 모든 예약 조회 가능
        if (user.getUserRole() == UserRole.ROLE_ADMIN) {
            return;
        }

        // 2. 본인 예약은 조회 가능
        if (reservation.isOwnedBy(userId)) {
            return;
        }

        // 3. 해당 병원의 관리자는 조회 가능
        if (user.getUserRole() == UserRole.ROLE_HOSPITAL
                && reservation.getHospital().getUserId().equals(userId)) {
            return;
        }

        // 4. 그 외는 조회 불가
        throw new GlobalException(ReservationErrorCode.NOT_HOSPITAL_ADMIN);
    }

    // 소유자 확인
    private void validateReservationOwner(Reservation reservation, Long userId) {
        if (!reservation.isOwnedBy(userId)) {
            throw new GlobalException(ReservationErrorCode.NOT_RESERVATION_OWNER);
        }
    }

    // 시간 검증
    private void validateAppointmentTime(Reservation reservation) {
        if (reservation.getAppointmentDate().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ReservationErrorCode.PAST_APPOINTMENT_TIME);
        }
    }
}