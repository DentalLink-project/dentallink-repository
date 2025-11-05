package com.dentallink.domain.reservation.repository;

import com.dentallink.domain.reservation.dto.ReservationCountDto;
import com.dentallink.domain.reservation.entity.Reservation;
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
            "WHERE r.hospital.id = :hospitalId " +
            "AND r.appointmentDate = :appointmentDate " +
            "AND r.status NOT IN ('CANCELLED', 'REJECTED') " +
            "AND r.deletedAt IS NULL")
    int countByHospitalIdAndAppointmentDate(
            @Param("hospitalId") Long hospitalId,
            @Param("appointmentDate") LocalDateTime appointmentDate
    );

    @Query("SELECT r FROM Reservation r " +
            "WHERE r.hospital.id = :hospitalId " +
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
            "WHERE r.user.id = :userId " +
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
            "WHERE r.user.id = :userId " +
            "AND r.deletedAt IS NULL " +
            "ORDER BY r.appointmentDate DESC")
    Page<Reservation> findByUserId(@Param("userId") Long userId, Pageable pageable);

    //병원의 특정일 예약 조회
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.hospital.id = :hospitalId " +
            "AND DATE(r.appointmentDate) = :date " +
            "AND r.status NOT IN ('CANCELLED', 'REJECTED') " +
            "AND r.deletedAt IS NULL")
    List<Reservation> findByHospitalIdAndDate(
            @Param("hospitalId") Long hospitalId,
            @Param("date") LocalDate date
    );

    //병원의 예약 목록 조회
    @Query("SELECT r FROM Reservation r " +
            "WHERE r.hospital.id = :hospitalId " +
            "AND r.deletedAt IS NULL " +
            "ORDER BY r.appointmentDate DESC")
    Page<Reservation> findByHospitalIdWithPaging(
            @Param("hospitalId") Long hospitalId,
            Pageable pageable);

    @Query("SELECT r.appointmentDate as timeSlot, COUNT(r.id) as count " +
            "FROM Reservation r " +
            "WHERE r.hospital.id = :hospitalId " +
            "AND r.appointmentDate BETWEEN :startDateTime AND :endDateTime " +
            "AND r.status NOT IN ('CANCELLED', 'REJECTED') " +
            "AND r.deletedAt IS NULL " +
            "GROUP BY r.appointmentDate")
    List<ReservationCountDto> countReservationsByTimeSlot(
            @Param("hospitalId") Long hospitalId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query(value = "SELECT r FROM Reservation r " +
            "JOIN FETCH r.hospital h " +
            "JOIN FETCH r.user u " +
            "WHERE r.user.id = :userId " +
            "AND r.deletedAt IS NULL " +
            "ORDER BY r.appointmentDate DESC",
            countQuery = "SELECT COUNT(r) FROM Reservation r " +
                    "WHERE r.user.id = :userId " +
                    "AND r.deletedAt IS NULL")
    Page<Reservation> findByUserIdWithFetchJoin(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT r FROM Reservation r " +
            "JOIN FETCH r.hospital h " +
            "JOIN FETCH r.user u " +
            "WHERE r.hospital.id = :hospitalId " +
            "AND r.deletedAt IS NULL " +
            "ORDER BY r.appointmentDate DESC",
            countQuery = "SELECT COUNT(r) FROM Reservation r " +
                    "WHERE r.hospital.id = :hospitalId " +
                    "AND r.deletedAt IS NULL")
    Page<Reservation> findByHospitalIdWithFetchJoin (@Param("hospitalId") Long hospitalId, Pageable pageable);

    boolean existsByHospitalIdAndAppointmentDate(Long hospitalId, LocalDateTime appointmentDate);
}