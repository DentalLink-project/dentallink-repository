/*package com.dentallink.domain.reservation.service;


import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.reservation.dto.ReservationCreateRequest;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.enums.ReservationStatus;
import com.dentallink.domain.reservation.execption.ReservationErrorCode;
import com.dentallink.domain.reservation.repository.ReservationRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationInternalService - 단위 테스트")
class ReservationInternalServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private HospitalScheduleRepository hospitalScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PointAccountExternalService pointAccountExternalService;

    @InjectMocks
    private ReservationInternalService reservationInternalService;

    private User user;
    private Hospital hospital;
    private HospitalSchedule schedule;
    private PointAccount pointAccount;
    private LocalDateTime appointmentDate;

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2025, 10, 22, 10, 0, 0);

    @BeforeEach
    void setUp() {
        // User 생성 및 ID 설정
        user = User.of(
                "test@naver.com",
                "password123",
                "테스트유저",
                UserRole.ROLE_USER
        );
        ReflectionTestUtils.setField(user, "id", 1L);

        // Hospital 생성
        hospital = new Hospital(
                1L,
                "테스트치과",
                "좋은 치과입니다",
                "서울시 강남구",
                true,
                "김의사"
        );

        // HospitalSchedule 생성
        schedule = HospitalSchedule.create(
                hospital,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );

        // PointAccount 생성
        pointAccount = PointAccount.create(user, 5000L);
        ReflectionTestUtils.setField(pointAccount, "id", 1L);

        appointmentDate = FIXED_NOW.plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("예약 생성 성공 - 포인트 차감 후 예약 저장 순서 검증")
    void createReservation_Success() {
        // given
        ReservationCreateRequest request = new ReservationCreateRequest(1L, appointmentDate);

        given(hospitalRepository.findById(1L)).willReturn(Optional.of(hospital));
        given(hospitalScheduleRepository.findByHospitalId(1L)).willReturn(Optional.of(schedule));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(reservationRepository.countByHospitalIdAndAppointmentDate(1L, appointmentDate)).willReturn(0);
        given(reservationRepository.existsByUserIdAndAppointmentDate(1L, appointmentDate)).willReturn(false);
        given(pointAccountExternalService.getPointAccountByUser(user)).willReturn(pointAccount);

        Reservation savedReservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        given(reservationRepository.save(any(Reservation.class))).willReturn(savedReservation);

        // when
        ReservationResponse response = reservationInternalService.createReservation(request, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.usedPoints()).isEqualTo(1000L);

        InOrder inOrder = inOrder(pointAccountExternalService, reservationRepository);
        then(pointAccountExternalService).should(inOrder).spendPointAccount(eq(1L), eq(1000L));
        then(reservationRepository).should(inOrder).save(any(Reservation.class));
    }

    @Test
    @DisplayName("예약 생성 실패 - 포인트 부족")
    void createReservation_InsufficientPoints() {
        // given
        ReservationCreateRequest request = new ReservationCreateRequest(1L, appointmentDate);

        given(hospitalRepository.findById(1L)).willReturn(Optional.of(hospital));
        given(hospitalScheduleRepository.findByHospitalId(1L)).willReturn(Optional.of(schedule));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(reservationRepository.countByHospitalIdAndAppointmentDate(1L, appointmentDate)).willReturn(0);
        given(reservationRepository.existsByUserIdAndAppointmentDate(1L, appointmentDate)).willReturn(false);
        given(pointAccountExternalService.getPointAccountByUser(user)).willReturn(pointAccount);

        willThrow(new IllegalStateException("잔액이 부족합니다."))
                .given(pointAccountExternalService)
                .spendPointAccount(eq(1L), eq(1000L));

        // when, then
        assertThatThrownBy(() -> reservationInternalService.createReservation(request, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.INSUFFICIENT_POINTS);

        then(reservationRepository).should(never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("예약 취소 성공 - 포인트 환불")
    void cancelReservation_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        given(reservationRepository.findByIdAndNotDeleted(1L)).willReturn(Optional.of(reservation));
        given(pointAccountExternalService.getPointAccountByUser(user)).willReturn(pointAccount);

        // when
        reservationInternalService.cancelReservation(1L, 1L);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        then(pointAccountExternalService).should(times(1))
                .refundPointAccount(eq(1L), eq(1000L));
    }

    @Test
    @DisplayName("예약 취소 실패 - 완료된 예약은 환불 불가")
    void cancelReservation_CompletedReservation_NoRefund() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);
        reservation.approve();
        reservation.complete();

        given(reservationRepository.findByIdAndNotDeleted(1L)).willReturn(Optional.of(reservation));

        // when, then
        assertThatThrownBy(() -> reservationInternalService.cancelReservation(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("완료/취소/거부된 예약은 취소할 수 없습니다");

        then(pointAccountExternalService).should(never()).refundPointAccount(any(), any());
    }

    @Test
    @DisplayName("예약 취소 실패 - 다른 사용자의 예약")
    void cancelReservation_NotOwner() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        given(reservationRepository.findByIdAndNotDeleted(1L)).willReturn(Optional.of(reservation));

        // when, then
        assertThatThrownBy(() -> reservationInternalService.cancelReservation(1L, 999L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.NOT_RESERVATION_OWNER);

        then(pointAccountExternalService).should(never()).refundPointAccount(any(), any());
    }

    @Test
    @DisplayName("예약 생성 실패 - 과거 시간")
    void createReservation_PastDateTime() {
        // given
        LocalDateTime pastDate = FIXED_NOW.minusDays(1);
        ReservationCreateRequest request = new ReservationCreateRequest(1L, pastDate);

        given(hospitalRepository.findById(1L)).willReturn(Optional.of(hospital));

        // when, then
        assertThatThrownBy(() -> reservationInternalService.createReservation(request, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.PAST_APPOINTMENT_TIME);
    }

    @Test
    @DisplayName("예약 생성 실패 - 병원 영업 중이 아님")
    void createReservation_HospitalClosed() {
        // given
        Hospital closedHospital = new Hospital(1L, "테스트치과", "좋은 치과", "서울시", false, "김의사");
        ReservationCreateRequest request = new ReservationCreateRequest(1L, appointmentDate);

        given(hospitalRepository.findById(1L)).willReturn(Optional.of(closedHospital));

        // when, then
        assertThatThrownBy(() -> reservationInternalService.createReservation(request, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.HOSPITAL_CLOSED);
    }

    @Test
    @DisplayName("예약 생성 실패 - 시간대 예약 마감")
    void createReservation_TimeSlotFull() {
        // given
        ReservationCreateRequest request = new ReservationCreateRequest(1L, appointmentDate);

        given(hospitalRepository.findById(1L)).willReturn(Optional.of(hospital));
        given(hospitalScheduleRepository.findByHospitalId(1L)).willReturn(Optional.of(schedule));
        given(reservationRepository.countByHospitalIdAndAppointmentDate(1L, appointmentDate)).willReturn(3);

        // when, then
        assertThatThrownBy(() -> reservationInternalService.createReservation(request, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.RESERVATION_FULL);
    }

    @Test
    @DisplayName("예약 생성 실패 - 중복 예약")
    void createReservation_DuplicateReservation() {
        // given
        ReservationCreateRequest request = new ReservationCreateRequest(1L, appointmentDate);

        given(hospitalRepository.findById(1L)).willReturn(Optional.of(hospital));
        given(hospitalScheduleRepository.findByHospitalId(1L)).willReturn(Optional.of(schedule));
        given(reservationRepository.countByHospitalIdAndAppointmentDate(1L, appointmentDate)).willReturn(0);
        given(reservationRepository.existsByUserIdAndAppointmentDate(1L, appointmentDate)).willReturn(true);

        // when, then
        assertThatThrownBy(() -> reservationInternalService.createReservation(request, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.DUPLICATE_RESERVATION);
    }
}*/