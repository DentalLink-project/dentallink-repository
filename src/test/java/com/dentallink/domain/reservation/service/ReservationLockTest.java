package com.dentallink.domain.reservation.service;

import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.hospital.entity.HospitalReservationTime;
import com.dentallink.domain.hospital.entity.HospitalSchedule;
import com.dentallink.domain.hospital.repository.HospitalRepository;
import com.dentallink.domain.hospital.repository.HospitalReservationTimeRepository;
import com.dentallink.domain.hospital.repository.HospitalScheduleRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.reservation.dto.ReservationCreateRequest;
import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.repository.ReservationRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReservationLockTest {

    @Autowired
    private ReservationInternalService reservationService;
    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private HospitalScheduleRepository hospitalScheduleRepository;
    @Autowired
    private HospitalReservationTimeRepository reservationTimeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Test
    @DisplayName("실제 DB 동시성 재현: 같은 타임슬롯 동시 예약 시 충돌 발생")
    void realConcurrentReservationTest() throws Exception {
        // given
        Hospital hospital = new Hospital(
                1L,
                "바른치과",
                "심미치료 전문. 토/일 예약 가능.",
                "서울 강남구",
                true,
                "강강강",
                1000L
        );
        hospitalRepository.saveAndFlush(hospital);

        HospitalSchedule schedule = new HospitalSchedule(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                hospital
        );
        hospitalScheduleRepository.saveAndFlush(schedule);

        HospitalReservationTime slot = new HospitalReservationTime(
                hospital,
                LocalDate.of(2025, 11, 7),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30)
        );
        reservationTimeRepository.saveAndFlush(slot);

        User user = userRepository.saveAndFlush(
                User.of("test@example.com", "password123!A", "테스트유저", UserRole.ROLE_USER)
        );

        PointAccount pointAccount = PointAccount.create(user, 10_000L); // 잔액 1만 원 예시
        pointAccountRepository.saveAndFlush(pointAccount);
        LocalDateTime appointmentDate = LocalDateTime.of(2025, 11, 7, 10, 0);
        ReservationCreateRequest request = new ReservationCreateRequest(hospital.getId(), appointmentDate);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    createReservation(request, user.getId());
                } catch (Exception e) {
                    System.out.println("예외 발생: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // 테스트 종료 후 ExecutorService 정리 (리소스 누수 방지)
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        // then
        List<Reservation> all = reservationRepository.findAll();
        System.out.println("총 예약 개수: " + all.size());

        PointAccount latestAccount = pointAccountRepository.findById(pointAccount.getId())
                .orElseThrow(() -> new IllegalStateException("포인트 계좌 없음"));
        System.out.println("최종 잔액: " + latestAccount.getBalance() + "원");

        //assertThat(all.size()).isGreaterThan(1);
        assertThat(all.size()).isEqualTo(1);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Commit
    void createReservation(ReservationCreateRequest request, Long userId) {
        reservationService.createReservation(request, userId);
    }
}
