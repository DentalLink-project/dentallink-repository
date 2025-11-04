package com.dentallink.domain.hospital.repository;

import com.dentallink.domain.hospital.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Page<Hospital> findAllByHospitalNameContainingAndDeletedIsFalse(Pageable pageable, String keyword);
}
