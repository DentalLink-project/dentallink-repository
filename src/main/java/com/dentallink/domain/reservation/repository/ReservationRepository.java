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
