package com.dentallink.hospital.repository;

import com.dentallink.hospital.entity.HospitalSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalScheduleRepository extends JpaRepository<HospitalSchedule, Integer> {
}
