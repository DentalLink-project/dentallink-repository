package com.dentallink.domain.hospital.repository;

import com.dentallink.domain.hospital.dto.response.HospitalDetailResponse;
import com.dentallink.domain.hospital.dto.response.HospitalListResponse;
import com.dentallink.domain.hospital.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Page<Hospital> findAllByHospitalNameContainingAndDeletedIsFalse(Pageable pageable, String keyword);

    @Query(value = """
    SELECT new com.dentallink.domain.hospital.dto.response.HospitalListResponse(
            h.id,
            h.hospitalName,
            h.doctorName,
            h.hospitalIsOpen,
            (f.id IS NOT NULL)
        )
        FROM Hospital h
        LEFT JOIN Favorite f
            ON f.hospital = h
            AND f.user.id = :userId
        ORDER BY h.id DESC
    """,
            countQuery = "SELECT COUNT(h) FROM Hospital h")
    Page<HospitalListResponse> findAllWithOptionalFavorite(@Param("userId") Long userId, Pageable pageable);
    @Query("""
    SELECT new com.dentallink.domain.hospital.dto.response.HospitalDetailResponse(
            h.id,
            h.hospitalName,
            h.hospitalDescription,
            h.hospitalAddress,
            h.hospitalIsOpen,
            h.doctorName,
            h.reservationCost,
            s.openTime,
            s.closeTime,
            s.breakStart,
            s.breakEnd,
            (f.id IS NOT NULL)
        )
        FROM Hospital h
        LEFT JOIN h.hospitalSchedule s
        LEFT JOIN Favorite f
            ON f.hospital = h
            AND f.user.id = :userId
        WHERE h.id = :hospitalId
    """)
    Optional<HospitalDetailResponse> findHospitalDetailWithOptionalFavorite(
            @Param("hospitalId") Long hospitalId,
            @Param("userId") Long userId
    );
}
