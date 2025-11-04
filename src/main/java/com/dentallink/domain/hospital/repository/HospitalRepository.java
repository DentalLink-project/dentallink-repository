package com.dentallink.domain.hospital.repository;

import com.dentallink.domain.hospital.dto.response.HospitalListResponse;
import com.dentallink.domain.hospital.entity.Hospital;
import com.dentallink.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    // 로그인한 사용자 기준 즐겨찾기 여부 포함 조회 (JPQL)
    @Query("SELECT new com.dentallink.domain.hospital.dto.response.HospitalListResponse(" +
            "h.id, " +
            "h.hospitalName, " +
            "h.doctorName, " +
            "h.hospitalIsOpen, " +
            "CASE WHEN f.id IS NOT NULL THEN true ELSE false END" +
            ") " +
            "FROM Hospital h " +
            "LEFT JOIN Favorite f ON f.hospital = h AND f.user = :user")
    List<HospitalListResponse> findAllWithFavorite(@Param("user") User user);

    // 로그인하지 않은 사용자일 때 단순 병원 목록 조회
    @Query("SELECT new com.dentallink.domain.hospital.dto.response.HospitalListResponse(" +
            "h.id, " +
            "h.hospitalName, " +
            "h.doctorName, " +
            "h.hospitalIsOpen, " +
            "false" +
            ") " +
            "FROM Hospital h")
    List<HospitalListResponse> findAllWithoutFavorite();
}
