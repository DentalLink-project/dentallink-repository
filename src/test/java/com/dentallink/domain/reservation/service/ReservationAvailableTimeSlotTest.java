package com.dentallink.domain.reservation.service;

import com.dentallink.common.exception.GlobalException;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.reservation.dto.AvailableTimeSlotResponse;
import com.dentallink.domain.reservation.exception.ReservationErrorCode;
import com.dentallink.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationInternalService - 예약 가능 시간대 조회 테스트")
class ReservationAvailableTimeSlotTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private HospitalScheduleRepository hospitalScheduleRepository;

    @InjectMocks
    private ReservationInternalService reservationService;

    private Hospital hospital;
    private HospitalSchedule schedule;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        // Hospital 생성
        hospital = new Hospital("테스트치과", "좋은 치과", "서울시", true, "김의사", 1000L);
        ReflectionTestUtils.setField(hospital, "id", 1L);

        // HospitalSchedule 생성 (9:00 ~ 18:00, 점심시간 12:00 ~ 13:00)
        schedule = HospitalSchedule.create(
                hospital,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );

        targetDate = LocalDateTime.now().plusDays(1).toLocalDate();
    }

    // ==================== 기본 시간대 조회 테스트 ====================

    @Test
    @DisplayName("예약 가능 시간대 조회 성공 - 모든 시간대 가능")
    void getAvailableTimePeriod_AllAvailable() {
        // given
        given(hospitalRepository.findById(1L)).willReturn(Optional.of(hospital));
        given(hospitalScheduleRepository.findByHospitalId(1L)).willReturn(Optional.of(schedule));
        given(reservationRepository.countReservationsByTimeSlot(1L,
                targetDate.atStartOfDay(),
                targetDate.atTime(LocalTime.MAX))).willReturn(new ArrayList<>());

        // when
        List<AvailableTimeSlotResponse> result = reservationService.getAvailableTimePeriod(1L, targetDate);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        // 최소한 몇 개의 시간대가 있어야 함
        assertThat(result.size()).isGreaterThan(0);
    }

    // ==================== 병원 상태 검증 ====================

    @Test
    @DisplayName("예약 가능 시간대 조회 실패 - 병원 폐쇄")
    void getAvailableTimePeriod_HospitalClosed() {
        // given
        Hospital closedHospital = new Hospital("폐쇄치과", "폐쇄됨", "서울시", false, "의사", 1000L);
        ReflectionTestUtils.setField(closedHospital, "id", 1L);

        given(hospitalRepository.findById(1L)).willReturn(Optional.of(closedHospital));

        // when, then
        assertThatThrownBy(() -> reservationService.getAvailableTimePeriod(1L, targetDate))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.HOSPITAL_CLOSED);
    }

    @Test
    @DisplayName("예약 가능 시간대 조회 실패 - 병원 없음")
    void getAvailableTimePeriod_HospitalNotFound() {
        // given
        given(hospitalRepository.findById(999L)).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> reservationService.getAvailableTimePeriod(999L, targetDate))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.HOSPITAL_NOT_FOUND);
    }

    @Test
    @DisplayName("예약 가능 시간대 조회 실패 - 스케줄 없음")
    void getAvailableTimePeriod_ScheduleNotFound() {
        // given
        given(hospitalRepository.findById(1L)).willReturn(Optional.of(hospital));
        given(hospitalScheduleRepository.findByHospitalId(1L)).willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> reservationService.getAvailableTimePeriod(1L, targetDate))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ReservationErrorCode.HOSPITAL_SCHEDULE_NOT_FOUND);
    }
}
