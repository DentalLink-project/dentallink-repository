package com.dentallink.domain.reservation.entity;

import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.reservation.enums.ReservationStatus;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Reservation - 엔티티 단위 테스트")
class ReservationTest {

    private User user;
    private Hospital hospital;
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

        appointmentDate = LocalDateTime.of(2025, 11, 15, 14, 0, 0);
    }

    // ==================== 생성 테스트 ====================

    @Test
    @DisplayName("예약 생성 성공 - 정상적인 예약 객체 생성")
    void testCreateReservation_Success() {
        // when
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);

        // then
        assertThat(reservation).isNotNull();
        assertThat(reservation.getHospital()).isEqualTo(hospital);
        assertThat(reservation.getUser()).isEqualTo(user);
        assertThat(reservation.getAppointmentDate()).isEqualTo(appointmentDate);
        assertThat(reservation.getUsedPoints()).isEqualTo(1000L);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("예약 생성 - 포인트가 0인 경우")
    void testCreateReservation_ZeroPoints() {
        // when
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 0L);

        // then
        assertThat(reservation.getUsedPoints()).isEqualTo(0L);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    // ==================== 상태 전이 테스트 ====================

    @Test
    @DisplayName("상태 변경: PENDING -> APPROVED")
    void testApproveReservation_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);

        // when
        reservation.approve();

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    @DisplayName("상태 변경 실패: PENDING이 아닌 상태에서 승인 시도")
    void testApproveReservation_InvalidStatus() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.approve(); // 이미 APPROVED 상태로 변경

        // when, then
        assertThatThrownBy(reservation::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("대기 중인 예약만 승인할 수 있습니다");
    }

    @Test
    @DisplayName("상태 변경: PENDING -> REJECTED")
    void testRejectReservation_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);

        // when
        reservation.reject();

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
    }

    @Test
    @DisplayName("상태 변경 실패: PENDING이 아닌 상태에서 거부 시도")
    void testRejectReservation_InvalidStatus() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.approve(); // APPROVED 상태로 변경

        // when, then
        assertThatThrownBy(reservation::reject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("대기 중인 예약만 거부할 수 있습니다");
    }

    @Test
    @DisplayName("상태 변경: APPROVED -> COMPLETED")
    void testCompleteReservation_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.approve();

        // when
        reservation.complete();

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    @DisplayName("상태 변경 실패: APPROVED가 아닌 상태에서 완료 시도")
    void testCompleteReservation_InvalidStatus() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        // PENDING 상태에서 완료 시도

        // when, then
        assertThatThrownBy(reservation::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("승인된 예약만 완료할 수 있습니다");
    }

    @Test
    @DisplayName("상태 변경: PENDING -> CANCELLED (취소)")
    void testCancelReservation_Success() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);

        // when
        reservation.cancel();

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.isDeleted()).isTrue(); // soft delete 확인
    }

    @Test
    @DisplayName("상태 변경 실패: COMPLETED 상태에서 취소 시도")
    void testCancelReservation_CompletedStatus() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.approve();
        reservation.complete();

        // when, then
        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("완료/취소/거부된 예약은 취소할 수 없습니다");
    }

    @Test
    @DisplayName("상태 변경 실패: CANCELLED 상태에서 취소 시도")
    void testCancelReservation_AlreadyCancelled() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.cancel();

        // when, then
        // soft delete되었으므로 삭제된 예약 에러가 발생
        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("삭제된 예약은 상태 변경할 수 없습니다");
    }

    @Test
    @DisplayName("상태 변경 실패: REJECTED 상태에서 취소 시도")
    void testCancelReservation_RejectedStatus() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.reject();

        // when, then
        assertThatThrownBy(reservation::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("완료/취소/거부된 예약은 취소할 수 없습니다");
    }

    @Test
    @DisplayName("상태 변경 실패: 삭제된 예약은 상태 변경 불가")
    void testStatusChange_DeletedReservation() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.cancel(); // soft delete

        // when, then
        assertThatThrownBy(reservation::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("삭제된 예약은 상태 변경할 수 없습니다");
    }

    // ==================== 소유권 및 관계 검증 테스트 ====================

    @Test
    @DisplayName("isOwnedBy - 소유자일 때 true 반환")
    void testIsOwnedBy_Owner() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);

        // when, then
        assertThat(reservation.isOwnedBy(user.getId())).isTrue();
    }

    @Test
    @DisplayName("isOwnedBy - 소유자가 아닐 때 false 반환")
    void testIsOwnedBy_NotOwner() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);

        // when, then
        assertThat(reservation.isOwnedBy(999L)).isFalse();
    }

    @Test
    @DisplayName("belongsToHospital - 해당 병원 예약일 때 true 반환")
    void testBelongsToHospital_Same() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);

        // when, then
        assertThat(reservation.belongsToHospital(hospital.getId())).isTrue();
    }

    @Test
    @DisplayName("belongsToHospital - 다른 병원 예약일 때 false 반환")
    void testBelongsToHospital_Different() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);

        // when, then
        assertThat(reservation.belongsToHospital(999L)).isFalse();
    }

    // ==================== 환불 가능성 테스트 ====================

    @Test
    @DisplayName("isRefundable - PENDING 상태는 환불 가능")
    void testIsRefundable_Pending() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);

        // when, then
        assertThat(reservation.isRefundable()).isTrue();
    }

    @Test
    @DisplayName("isRefundable - APPROVED 상태는 환불 가능")
    void testIsRefundable_Approved() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.approve();

        // when, then
        assertThat(reservation.isRefundable()).isTrue();
    }

    @Test
    @DisplayName("isRefundable - REJECTED 상태는 환불 가능")
    void testIsRefundable_Rejected() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.reject();

        // when, then
        assertThat(reservation.isRefundable()).isTrue();
    }

    @Test
    @DisplayName("isRefundable - COMPLETED 상태는 환불 불가")
    void testIsRefundable_Completed() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 1000L);
        reservation.approve();
        reservation.complete();

        // when, then
        assertThat(reservation.isRefundable()).isFalse();
    }

    @Test
    @DisplayName("getRefundablePoints - 환불 가능 상태에서는 사용한 포인트 반환")
    void testGetRefundablePoints_Refundable() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 5000L);

        // when
        Long refundablePoints = reservation.getRefundablePoints();

        // then
        assertThat(refundablePoints).isEqualTo(5000L);
    }

    @Test
    @DisplayName("getRefundablePoints - COMPLETED 상태에서는 0 반환")
    void testGetRefundablePoints_Completed() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 5000L);
        reservation.approve();
        reservation.complete();

        // when
        Long refundablePoints = reservation.getRefundablePoints();

        // then
        assertThat(refundablePoints).isEqualTo(0L);
    }

    @Test
    @DisplayName("getRefundablePoints - 포인트 0인 경우")
    void testGetRefundablePoints_ZeroPoints() {
        // given
        Reservation reservation = Reservation.create(hospital, user, appointmentDate, 0L);

        // when
        Long refundablePoints = reservation.getRefundablePoints();

        // then
        assertThat(refundablePoints).isEqualTo(0L);
    }
}
