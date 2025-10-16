package com.dentallink.reservation.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.reservation.enums.ReservationStatus;
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

    //TODO: 병원 엔티티 생성 후 주석 해제 및 병원아이디 삭제
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "hospital_id", nullable = false)
//    private Hospital hospital;

    @Column(name = "hospital_id", nullable = false)
    private Long hospitalId;

    //TODO: User Entity 새성 후 주서거 해제하고 userId 제거
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    public static Reservation create(Long hospitalId, Long userId, LocalDateTime appointmentDate) {
        Reservation reservation = new Reservation();
        reservation.hospitalId = hospitalId;
        reservation.userId = userId;
        reservation.appointmentDate = appointmentDate;
        reservation.status = ReservationStatus.PENDING;
        return reservation;
    }

    //TODO: 병원, 유저 엔티티 생성 하고 하면 위 create 삭제하고 아래 주석 해제
//    public static Reservation create(Hospital hospital, User user, LocalDateTime appointmentDate) {
//        Reservation reservation = new Reservation();
//        reservation.hospital = hospital;
//        reservation.user = user;
//        reservation.appointmentDate = appointmentDate;
//        reservation.status = ReservationStatus.PENDING;
//        return reservation;
//    }


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
