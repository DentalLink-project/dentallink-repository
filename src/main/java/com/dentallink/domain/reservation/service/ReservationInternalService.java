package com.dentallink.domain.reservation.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.lock.DistributedLock; // ✅ (분산락) 락 어노테이션 import 추가
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
import org.springframework.transaction.annotation.Propagation;
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

    //예약 조회 (단건)
    public ReservationResponse getReservation(Long id, Long userId) {
        Reservation reservation = reservationRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        validateReservationAccess(reservation, userId);
        return ReservationResponse.from(reservation);
    }

    //내 예약 목록 조회
    public Page<ReservationResponse> getMyReservations(Long userId, Pageable pageable) {
        Page<Reservation> reservations = reservationRepository.findByUserIdWithFetchJoin(userId, pageable);
        return reservations.map(ReservationResponse::from);
    }

    //병원 예약 목록 조회
    public Page<ReservationResponse> getHospitalReservations(Long hospitalId, Long hospitalAdminId, Pageable pageable) {
        validateHospitalOwnership(hospitalId, hospitalAdminId);
        Page<Reservation> reservations = reservationRepository.findByHospitalIdWithFetchJoin(hospitalId, pageable);
        return reservations.map(ReservationResponse::from);
    }

    //예약 상태 변경
    @Transactional
    public ReservationResponse updateReservationStatus(Long id, ReservationUpdateStatusRequest request, Long hospitalAdminId) {
        Reservation reservation = reservationRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        // 병원 소유권 확인 (@PreAuthorize로 역할은 체크됨)
        validateHospitalOwnership(reservation.getHospital().getId(), hospitalAdminId);

        switch (request.status()) {
            case APPROVED -> reservation.approve();
            case REJECTED -> {
                Long refundablePoints = reservation.getRefundablePoints();
                reservation.reject();
                refundPoints(reservation.getUser().getId(), refundablePoints);
            }
            case COMPLETED -> reservation.complete();
            default -> throw new GlobalException(ReservationErrorCode.INVALID_STATUS_TRANSITION);
        }

        return ReservationResponse.from(reservation);
    }

    //예약 취소 - 포인트 환불
    @Transactional
    public void cancelReservation(Long id, Long userId) {
        Reservation reservation = reservationRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        validateReservationOwner(reservation, userId);
        validateAppointmentTime(reservation);

        Long refundablePoints = reservation.getRefundablePoints();
        reservation.cancel();
        refundPoints(reservation.getUser().getId(), refundablePoints);
    }

    /**
     * 예약 생성 (분산락 적용)
     * 병원 ID + 예약 시간대 조합으로 락을 걸어 중복 예약 방지
     */
    @DistributedLock(
            key = "'reservation:' + #request.hospitalId() + ':' + "
                    + "T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMddHHmm').format(#request.appointmentDate)",
            waitTime = 10,
            leaseTime = 15
    )

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservationResponse createReservation(ReservationCreateRequest request, Long userId) {

        // (분산락) — Redisson이 reservation:{hospitalId}:{time} 키로 락을 획득한 상태에서만 아래 코드가 실행됨

        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_NOT_FOUND));

        validateHospitalIsOpen(hospital);
        validateAppointmentDateTime(request.appointmentDate());

        HospitalSchedule schedule = hospitalScheduleRepository.findByHospitalId(request.hospitalId())
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));

        validateBusinessHours(request.appointmentDate(), schedule);
        validateTimeSlotAvailability(request.hospitalId(), request.appointmentDate());
        validateDuplicateUserReservation(userId, request.appointmentDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.USER_NOT_FOUND));

        // 예약비가 있고 0보다 클 때만 포인트 차감
        if (hospital.getReservationCost() != null && hospital.getReservationCost() > 0) {
            try {
                PointAccount pointAccount = pointAccountExternalService.getPointAccountByUserId(user.getId());
                pointAccountExternalService.spendPointAccount(pointAccount.getId(), hospital.getReservationCost());
            } catch (IllegalStateException e) {
                throw new GlobalException(ReservationErrorCode.INSUFFICIENT_POINTS);
            }
        }

        Reservation reservation = Reservation.create(
                hospital,
                user,
                request.appointmentDate(),
                hospital.getReservationCost()
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

        Map<LocalDateTime, Long> reservationCountMap = reservationCounts.stream()
                .collect(Collectors.toMap(
                        ReservationCountDto::getTimeSlot,
                        ReservationCountDto::getCount
                ));

        List<AvailableTimeSlotResponse> availableTimeSlots = new ArrayList<>();

        for (LocalDateTime timeSlot : timesPeriod) {
            long existingCount = reservationCountMap.getOrDefault(timeSlot, 0L);
            int availableCount = MAX_RESERVATIONS_PER_TIME_SLOT - (int) existingCount;

            availableTimeSlots.add(AvailableTimeSlotResponse.of(timeSlot, Math.max(0, availableCount)));
        }

        return availableTimeSlots;
    }

    // ======================= 유효성 및 공통 메서드 =======================

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
        int currentReservationCount = reservationRepository.countByHospitalIdAndAppointmentDate(hospitalId, appointmentDate);
        if (currentReservationCount >= MAX_RESERVATIONS_PER_TIME_SLOT) {
            throw new GlobalException(ReservationErrorCode.RESERVATION_FULL);
        }
    }

    private void validateDuplicateUserReservation(Long userId, LocalDateTime appointmentDate) {
        boolean exists = reservationRepository.existsByUserIdAndAppointmentDate(userId, appointmentDate);
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
     *
     * @PreAuthorize로 역할은 이미 체크되었으므로, 비즈니스 로직만 체크
     */
    private void validateHospitalOwnership(Long hospitalId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.USER_NOT_FOUND));

        if (user.getUserRole() == UserRole.ROLE_ADMIN) return;

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
        if (user.getUserRole() == UserRole.ROLE_ADMIN) return;
        // 2. 본인 예약은 조회 가능
        if (reservation.isOwnedBy(userId)) return;
        // 3. 해당 병원의 관리자는 조회 가능
        if (user.getUserRole() == UserRole.ROLE_HOSPITAL
                && reservation.getHospital().getUserId().equals(userId)) return;

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

    // 포인트 환불 (중복 제거)
    private void refundPoints(Long userId, Long points) {
        if (points <= 0) return;
        PointAccount pointAccount = pointAccountExternalService.getPointAccountByUserId(userId);
        pointAccountExternalService.refundPointAccount(pointAccount.getId(), points);
    }
}
