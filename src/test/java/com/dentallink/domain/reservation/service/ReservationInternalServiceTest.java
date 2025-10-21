package com.dentallink.domain.reservation.service;


import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.reservation.dto.ReservationCreateRequest;
import com.dentallink.domain.reservation.dto.ReservationResponse;
import com.dentallink.domain.reservation.entity.Reservation;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationInternalService")
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

        user = User.of(
                "test@naver.com",
                "qlalfqjsgh3*",
                "박서준",
                UserRole.ROLE_USER
        );

        hospital = new Hospital(
                1L,
                "테스트 치과",
                "좋은 치과설명 들어가는 자리",
                "서울시 강남구",
                true,
                "김지원"
        );

        schedule = HospitalSchedule.create(
                hospital,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );

        pointAccount = PointAccount.create(user, 5000L);

        appointmentDate = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("예약 생성 성공 - 포인트 차감")
    void createReservation() {
        //given
        ReservationCreateRequest request = new ReservationCreateRequest(
                1L,
                appointmentDate
        );
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

        // 포인트 차감 확인
        then(pointAccountExternalService).should(times(1))
                .spendPointAccount(eq(pointAccount.getId()), eq(1000L));

        // 예약 저장 확인
        then(reservationRepository).should(times(1)).save(any(Reservation.class));
    }


}