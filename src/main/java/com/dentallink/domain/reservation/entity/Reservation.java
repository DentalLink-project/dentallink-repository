package com.dentallink.domain.reservation.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.reservation.enums.ReservationStatus;
import com.dentallink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservations")
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    // 포인트 사용 내역
    @Column(name = "used_points", nullable = false)
    private Long usedPoints;

    public static Reservation create(
            Hospital hospital,
            User user,
            LocalDateTime appointmentDate,
            Long usedPoints) {
        Reservation reservation = new Reservation();
        reservation.hospital = hospital;
        reservation.user = user;
        reservation.appointmentDate = appointmentDate;
        reservation.usedPoints = usedPoints;
        reservation.status = ReservationStatus.PENDING;
        return reservation;
    }

    public void approve() {
        validateNotDeleted();
        validateStatusForApprove();
        this.status = ReservationStatus.APPROVED;
    }

    public void reject() {
        validateNotDeleted();
        validateStatusForReject();
        this.status = ReservationStatus.REJECTED;
    }

    public void cancel() {
        validateNotDeleted();
        validateStatusForCancel();
        this.status = ReservationStatus.CANCELLED;
        this.delete(); // soft delete
    }

    public void complete() {
        validateNotDeleted();
        validateStatusForComplete();
        this.status = ReservationStatus.COMPLETED;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public boolean belongsToHospital(Long hospitalId) {
        return this.hospital.getId().equals(hospitalId);
    }

    public boolean isRefundable() {
        // 완료된 예약은 환불 불가
        return this.status != ReservationStatus.COMPLETED;
    }

    public Long getRefundablePoints() {
        if (!isRefundable()) {
            return 0L;
        }
        return this.usedPoints;
    }

    private void validateNotDeleted() {
        if (this.isDeleted()) {
            throw new IllegalStateException("삭제된 예약은 상태 변경할 수 없습니다");
        }
    }

    private void validateStatusForApprove() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 예약만 승인할 수 있습니다");
        }
    }

    private void validateStatusForReject() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException("대기 중인 예약만 거부할 수 있습니다");
        }
    }

    private void validateStatusForCancel() {
        if (this.status == ReservationStatus.COMPLETED ||
                this.status == ReservationStatus.CANCELLED ||
                this.status == ReservationStatus.REJECTED) {
            throw new IllegalStateException("완료/취소/거부된 예약은 취소할 수 없습니다");
        }
    }

    private void validateStatusForComplete() {
        if (this.status != ReservationStatus.APPROVED) {
            throw new IllegalStateException("승인된 예약만 완료할 수 있습니다");
        }
    }
}