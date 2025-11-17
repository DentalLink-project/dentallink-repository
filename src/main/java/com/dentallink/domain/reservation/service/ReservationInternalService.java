package com.dentallink.domain.reservation.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.common.lock.DistributedLock;
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

    //추가: 최소 예약 가능 시간 여유 (분 단위)
    private static final int MIN_RESERVATION_BUFFER_MINUTES = 30;

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
     */
    @DistributedLock(
            key = "'reservation:' + #request.hospitalId() + ':' + "
                    + "T(java.time.format.DateTimeFormatter).ofPattern('yyyyMMddHHmm').format(#request.appointmentDate)",
            waitTime = 10,
            leaseTime = 15
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservationResponse createReservation(ReservationCreateRequest request, Long userId) {

        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_NOT_FOUND));

        validateHospitalIsOpen(hospital);

        // 개선: 과거 시간 체크를 가장 먼저 수행
        validateAppointmentDateTime(request.appointmentDate());

        HospitalSchedule schedule = hospitalScheduleRepository.findByHospitalId(request.hospitalId())
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND));

        validateBusinessHours(request.appointmentDate(), schedule);
        validateTimeSlotAvailability(request.hospitalId(), request.appointmentDate());
        validateDuplicateUserReservation(userId, request.appointmentDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.USER_NOT_FOUND));

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

    /**
     * 개선: 과거 시간 체크 강화 + 최소 예약 여유 시간 추가
     */
    private void validateAppointmentDateTime(LocalDateTime appointmentDate) {
        LocalDateTime now = LocalDateTime.now();

        //1. 과거 시간 체크 (기본)
        if (appointmentDate.isBefore(now)) {
            throw new GlobalException(ReservationErrorCode.PAST_APPOINTMENT_TIME);
        }

        //2. 최소 예약 여유 시간 체크 (예: 30분 전에는 예약 불가)
        LocalDateTime minReservationTime = now.plusMinutes(MIN_RESERVATION_BUFFER_MINUTES);
        if (appointmentDate.isBefore(minReservationTime)) {
            throw new GlobalException(ReservationErrorCode.TOO_CLOSE_APPOINTMENT_TIME);
        }

        // 3. 시간 단위 체크 (30분 단위)
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

    /**
     * 개선: 예약 가능 시간대 생성 시 과거 시간 + 여유 시간 필터링
     */
    private List<LocalDateTime> generateTimePeriod(LocalDate date, HospitalSchedule schedule) {
        List<LocalDateTime> timePeriod = new ArrayList<>();
        LocalTime currentTime = schedule.getOpenTime();
        LocalTime lastSlot = schedule.getCloseTime().minusMinutes(TIME_PERIOD);

        // 현재 시간 + 여유 시간
        LocalDateTime minReservationTime = LocalDateTime.now().plusMinutes(MIN_RESERVATION_BUFFER_MINUTES);

        while (!currentTime.isAfter(lastSlot)) {
            // 휴게시간 체크
            if (schedule.getBreakStart() == null || schedule.getBreakEnd() == null ||
                    currentTime.isBefore(schedule.getBreakStart()) ||
                    !currentTime.isBefore(schedule.getBreakEnd())) {

                LocalDateTime timeSlot = LocalDateTime.of(date, currentTime);

                //개선: 최소 예약 여유 시간 이후만 추가
                if (!timeSlot.isBefore(minReservationTime)) {
                    timePeriod.add(timeSlot);
                }
            }
            currentTime = currentTime.plusMinutes(TIME_PERIOD);
        }
        return timePeriod;
    }

    private void validateHospitalOwnership(Long hospitalId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.USER_NOT_FOUND));

        if (user.getUserRole() == UserRole.ROLE_ADMIN) return;

        if (user.getHospitalId() == null || !user.getHospitalId().equals(hospitalId)) {
            throw new GlobalException(ReservationErrorCode.NOT_HOSPITAL_ADMIN);
        }
    }

    private void validateReservationAccess(Reservation reservation, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ReservationErrorCode.USER_NOT_FOUND));

        if (user.getUserRole() == UserRole.ROLE_ADMIN) return;
        if (reservation.isOwnedBy(userId)) return;
        if (user.getUserRole() == UserRole.ROLE_HOSPITAL
                && reservation.getHospital().getId().equals(user.getHospitalId())) return;

        throw new GlobalException(ReservationErrorCode.NOT_HOSPITAL_ADMIN);
    }

    private void validateReservationOwner(Reservation reservation, Long userId) {
        if (!reservation.isOwnedBy(userId)) {
            throw new GlobalException(ReservationErrorCode.NOT_RESERVATION_OWNER);
        }
    }

    private void validateAppointmentTime(Reservation reservation) {
        if (reservation.getAppointmentDate().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ReservationErrorCode.PAST_APPOINTMENT_TIME);
        }
    }

    private void refundPoints(Long userId, Long points) {
        if (points <= 0) return;
        PointAccount pointAccount = pointAccountExternalService.getPointAccountByUserId(userId);
        pointAccountExternalService.refundPointAccount(pointAccount.getId(), points);
    }
}