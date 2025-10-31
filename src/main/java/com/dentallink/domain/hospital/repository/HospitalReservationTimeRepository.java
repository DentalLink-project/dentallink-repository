package com.dentallink.domain.hospital.repository;

import com.dentallink.domain.hospital.entity.HospitalReservationTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface HospitalReservationTimeRepository extends JpaRepository<HospitalReservationTime, Long> {
    List<HospitalReservationTime> findByHospital_IdAndDateOrderByStartTime(Long hospitalId, LocalDate date);

    Optional<HospitalReservationTime> findByHospital_IdAndDateAndStartTime(Long hospitalId, LocalDate date, LocalTime startTime);

    boolean existsByHospital_IdAndDate(Long hospitalId, LocalDate date);
}