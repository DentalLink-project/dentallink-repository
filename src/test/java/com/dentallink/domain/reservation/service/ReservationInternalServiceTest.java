package com.dentallink.domain.reservation.service;


import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.reservation.repository.ReservationRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;

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
    private PointAccountExternalService pointAccountExternalService;

    @InjectMocks
    private ReservationInternalService reservationInternalService;

    private User user;
    private Hospital hospital;
    private HospitalSchedule hospitalschedule;
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

        hospitalschedule = HospitalSchedule.create(
                hospital,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );

        pointAccount = PointAccount.create(user, 5000L);

        appointmentDate = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0);
    }




}