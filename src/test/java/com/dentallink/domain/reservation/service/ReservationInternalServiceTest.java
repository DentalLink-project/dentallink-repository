package com.dentallink.domain.reservation.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.enums.ReservationStatus;
import com.dentallink.domain.reservation.exception.ReservationErrorCode;
import com.dentallink.domain.reservation.repository.ReservationRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationInternalService - 기본 단위 테스트")
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
                "테스트치과",
                "좋은 치과입니다",
                "서울시 강남구",
                true,
                "김의사",
                1000L
        );
        ReflectionTestUtils.setField(hospital, "id", 1L);

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

        // 현재 시간 기준으로 내일 14:00 설정
        appointmentDate = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
    }

    // ==================== 예약 취소 테스트 ====================

    @Test
    @DisplayName("예약 취소 성공 - 포인트 환불")
    void cancelReservation_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        given(reservationRepository.findByIdAndNotDeleted(1L)).willReturn(Optional.of(reservation));
        given(pointAccountExternalService.getPointAccountByUserId(1L)).willReturn(pointAccount);

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
    @DisplayName("예약 조회 실패 - 예약 없음")
    void getReservation_NotFound() {
        // given
        given(reservationRepository.findByIdAndNotDeleted(999L)).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> reservationInternalService.getReservation(999L, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.RESERVATION_NOT_FOUND);
    }
}
