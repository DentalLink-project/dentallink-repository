package com.dentallink.domain.reservation.repository;

import com.dentallink.domain.reservation.entity.Reservation;
import com.dentallink.domain.reservation.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT COUNT(r) FROM Reservation r " +
            "WHERE r.hospitalId = :hospitalId " +
            "AND r.appointmentDate = :appointmentDate " +
            "AND r.status NOT IN ('CANCELLED', 'REJECTED') " +
            "AND r.deletedAt IS NULL")
    int countByHospitalIdAndAppointmentDate(
            @Param("hospitalId") Long hospital,
            @Param("appointmentDate") LocalDateTime appointmentDate
    );

    @Query("SELECT r FROM Reservation r " +
            "WHERE r.hospitalId = :hospitalId " +
            "AND r.appointmentDate BETWEEN :startDateTime AND :endDateTime " +
            "AND r.status NOT IN ('CANCELLED', 'REJECTED') " +
            "AND r.deletedAt IS NULL")
    List<Reservation> findByHospitalIdAndDateRange(
            @Param("hospitalId") Long hospitalId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );


    //특정 시간대에 예약이 존재하는지 확인
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
            "WHERE r.userId = :userId " +
            "AND r.appointmentDate = :appointmentDate " +
            "AND r.status NOT IN ('CANCELLED', 'REJECTED') " +
            "AND r.deletedAt IS NULL")
    boolean existsByUserIdAndAppointmentDate(
            @Param("userId") Long userId,
            @Param("appointmentDate") LocalDateTime appointmentDate
    );

    //예약 단건 조회
    @Query("SELECT r FROM Reservation r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<Reservation> findByIdAndNotDeleted(@Param("id") Long id);

    //사용자의 예약 목록 조회
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.userId = :userId " +
            "AND r.deletedAt IS NULL " +
            "ORDER BY r.appointmentDate DESC")
    Page<Reservation> findByUserId(@Param("userId") Long longId, Pageable pageable);

    //병원의 특정일 예약 조회
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.hospitalId = :hospitalId " +
            "AND DATE(r.appointmentDate) = :date " +
            "AND r.status NOT IN ('CANCELLED', 'REJECTED') " +
            "AND r.deletedAt IS NULL")
    List<Reservation> findByHospitalIdAndDate(
            @Param("hospitalId") Long hospitalId,
            @Param("date") LocalDate date
    );

    //병원의 예약 목록 조회
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.hospitalId = :hospitalId " +
            "AND r.deletedAt IS NULL " +
            "ORDER BY r.appointmentDate DESC")
    Page<Reservation> findByHospitalIdWithPaging(
            @Param("hospitalId") Long hospitalId,
            Pageable pageable);

}
