package com.dentallink.domain.hospital.repository;

import com.dentallink.domain.hospital.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    @EntityGraph(attributePaths = "hospitalSchedule")
    @Query("""
    select h from Hospital h
    where h.deleted IS false
    and (
        :keyword IS NULL
        or h.hospitalName like concat('%', :keyword, '%')
        or h.hospitalAddress like concat('%', :keyword, '%')
        or h.doctorName like concat('%', :keyword, '%')
    )
    """)
    Page<Hospital> findAllWithHospitalSchedule(Pageable pageable, String keyword);
}
