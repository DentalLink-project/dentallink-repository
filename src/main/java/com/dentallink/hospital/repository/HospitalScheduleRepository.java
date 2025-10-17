package com.dentallink.hospital.repository;

import com.dentallink.hospital.entity.HospitalSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HospitalScheduleRepository extends JpaRepository<HospitalSchedule, Integer> {

    //병원 ID로 스케쥴 조회 석호
    @Query("SELECT hs FROM HospitalSchedule hs " +
            "WHERE hs.hospital.id = :hospitalId")
    Optional<HospitalSchedule> findByHospitalId(@Param("hospitalId") Long hospitalId);
}
