package com.dentallink.domain.hospital.repository;

import com.dentallink.domain.hospital.entity.HospitalAvailableTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HospitalAvailableTimeRepository extends JpaRepository<HospitalAvailableTime, Long> {
    List<HospitalAvailableTime> findByHospitalId(Long hospitalId, LocalDate date);
}
