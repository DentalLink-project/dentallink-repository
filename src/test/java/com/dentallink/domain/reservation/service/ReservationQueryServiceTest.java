package com.dentallink.domain.reservation.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.entity.Reservation;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationInternalService - 조회 기능 테스트")
class ReservationQueryServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private HospitalScheduleRepository hospitalScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationInternalService reservationService;

    private User user;
    private User otherUser;
    private User adminUser;
    private User hospitalAdminUser;
    private Hospital hospital;
    private Hospital otherHospital;
    private HospitalSchedule schedule;
    private LocalDateTime appointmentDate;


    @BeforeEach
    void setUp() {
        // User 설정
        user = User.of("test@naver.com", "password123", "테스트유저", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", 1L);

        otherUser = User.of("other@naver.com", "password123", "다른유저", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(otherUser, "id", 2L);

        adminUser = User.of("admin@naver.com", "password123", "관리자", UserRole.ROLE_ADMIN);
        ReflectionTestUtils.setField(adminUser, "id", 100L);

        hospitalAdminUser = User.of("hospital@naver.com", "password123", "병원관리자", UserRole.ROLE_HOSPITAL);
        ReflectionTestUtils.setField(hospitalAdminUser, "id", 50L);
        ReflectionTestUtils.setField(hospitalAdminUser, "hospitalId", 1L); // 병원 1 관리자

        // Hospital 설정
        hospital = new Hospital("테스트치과", "좋은 치과입니다", "서울시 강남구", true, "김의사", 1000L);
        ReflectionTestUtils.setField(hospital, "id", 1L);

        otherHospital = new Hospital("다른치과", "다른 치과", "서울시 강서구", true, "이의사", 1000L);
        ReflectionTestUtils.setField(otherHospital, "id", 2L);

        // HospitalSchedule 설정
        schedule = HospitalSchedule.create(
                hospital,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );

        appointmentDate = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0);
    }

    // ==================== 단건 조회 테스트 ====================

    @Test
    @DisplayName("예약 조회 성공 - 본인 예약 조회")
    void getReservation_ByOwner_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        given(reservationRepository.findByIdAndNotDeleted(1L)).willReturn(Optional.of(reservation));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        ReservationResponse response = reservationService.getReservation(1L, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("예약 조회 실패 - 다른 일반 사용자가 조회 시도")
    void getReservation_ByOtherUser_Failure() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        given(reservationRepository.findByIdAndNotDeleted(1L)).willReturn(Optional.of(reservation));
        given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));

        // when, then
        assertThatThrownBy(() -> reservationService.getReservation(1L, 2L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.NOT_HOSPITAL_ADMIN);
    }

    @Test
    @DisplayName("예약 조회 성공 - 병원 관리자가 자기 병원 예약 조회")
    void getReservation_ByHospitalAdmin_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        given(reservationRepository.findByIdAndNotDeleted(1L)).willReturn(Optional.of(reservation));
        given(userRepository.findById(50L)).willReturn(Optional.of(hospitalAdminUser));

        // when
        ReservationResponse response = reservationService.getReservation(1L, 50L);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("예약 조회 실패 - 다른 병원 관리자가 조회 시도")
    void getReservation_ByOtherHospitalAdmin_Failure() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        User otherHospitalAdmin = User.of("other_hospital@naver.com", "password123", "다른병원관리자", UserRole.ROLE_HOSPITAL);
        ReflectionTestUtils.setField(otherHospitalAdmin, "id", 60L);
        ReflectionTestUtils.setField(otherHospitalAdmin, "hospitalId", 2L); // 병원 2 관리자

        given(reservationRepository.findByIdAndNotDeleted(1L)).willReturn(Optional.of(reservation));
        given(userRepository.findById(60L)).willReturn(Optional.of(otherHospitalAdmin));

        // when, then
        assertThatThrownBy(() -> reservationService.getReservation(1L, 60L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.NOT_HOSPITAL_ADMIN);
    }

    @Test
    @DisplayName("예약 조회 성공 - 시스템 관리자가 모든 예약 조회 가능")
    void getReservation_BySystemAdmin_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        given(reservationRepository.findByIdAndNotDeleted(1L)).willReturn(Optional.of(reservation));
        given(userRepository.findById(100L)).willReturn(Optional.of(adminUser));

        // when
        ReservationResponse response = reservationService.getReservation(1L, 100L);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("예약 조회 실패 - 예약 없음")
    void getReservation_NotFound() {
        // given
        given(reservationRepository.findByIdAndNotDeleted(999L)).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> reservationService.getReservation(999L, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.RESERVATION_NOT_FOUND);
    }

    // ==================== 내 예약 목록 조회 테스트 ====================

    @Test
    @DisplayName("내 예약 목록 조회 성공")
    void getMyReservations_Success() {
        // given
        Reservation reservation1 = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation1, "id", 1L);

        Reservation reservation2 = Reservation.create(hospital, user, appointmentDate.plusDays(1), 1000L);
        ReflectionTestUtils.setField(reservation2, "id", 2L);

        List<Reservation> reservations = List.of(reservation1, reservation2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> page = new PageImpl<>(reservations, pageable, 2);

        given(reservationRepository.findByUserIdWithFetchJoin(1L, pageable)).willReturn(page);

        // when
        Page<ReservationResponse> result = reservationService.getMyReservations(1L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("내 예약 목록 조회 - 예약 없음")
    void getMyReservations_Empty() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);

        given(reservationRepository.findByUserIdWithFetchJoin(1L, pageable)).willReturn(emptyPage);

        // when
        Page<ReservationResponse> result = reservationService.getMyReservations(1L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    // ==================== 병원 예약 목록 조회 테스트 ====================

    @Test
    @DisplayName("병원 예약 목록 조회 성공 - 병원 관리자")
    void getHospitalReservations_ByHospitalAdmin_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        List<Reservation> reservations = List.of(reservation);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> page = new PageImpl<>(reservations, pageable, 1);

        given(userRepository.findById(50L)).willReturn(Optional.of(hospitalAdminUser));
        given(reservationRepository.findByHospitalIdWithFetchJoin(1L, pageable)).willReturn(page);

        // when
        Page<ReservationResponse> result = reservationService.getHospitalReservations(1L, 50L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("병원 예약 목록 조회 실패 - 권한 없는 사용자")
    void getHospitalReservations_ByUnauthorizedUser_Failure() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findById(2L)).willReturn(Optional.of(otherUser));

        // when, then
        assertThatThrownBy(() -> reservationService.getHospitalReservations(1L, 2L, pageable))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.NOT_HOSPITAL_ADMIN);
    }

    @Test
    @DisplayName("병원 예약 목록 조회 실패 - 다른 병원 관리자")
    void getHospitalReservations_ByOtherHospitalAdmin_Failure() {
        // given
        User otherHospitalAdmin = User.of("other_hospital@naver.com", "password123", "다른병원관리자", UserRole.ROLE_HOSPITAL);
        ReflectionTestUtils.setField(otherHospitalAdmin, "id", 60L);
        ReflectionTestUtils.setField(otherHospitalAdmin, "hospitalId", 2L); // 병원 2 관리자

        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findById(60L)).willReturn(Optional.of(otherHospitalAdmin));

        // when, then
        assertThatThrownBy(() -> reservationService.getHospitalReservations(1L, 60L, pageable))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.NOT_HOSPITAL_ADMIN);
    }

    @Test
    @DisplayName("병원 예약 목록 조회 성공 - 시스템 관리자")
    void getHospitalReservations_BySystemAdmin_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        ReflectionTestUtils.setField(reservation, "id", 1L);

        List<Reservation> reservations = List.of(reservation);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> page = new PageImpl<>(reservations, pageable, 1);

        given(userRepository.findById(100L)).willReturn(Optional.of(adminUser));
        given(reservationRepository.findByHospitalIdWithFetchJoin(1L, pageable)).willReturn(page);

        // when
        Page<ReservationResponse> result = reservationService.getHospitalReservations(1L, 100L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("병원 예약 목록 조회 - 예약 없음")
    void getHospitalReservations_Empty() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Reservation> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0);

        given(userRepository.findById(50L)).willReturn(Optional.of(hospitalAdminUser));
        given(reservationRepository.findByHospitalIdWithFetchJoin(1L, pageable)).willReturn(emptyPage);

        // when
        Page<ReservationResponse> result = reservationService.getHospitalReservations(1L, 50L, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}
