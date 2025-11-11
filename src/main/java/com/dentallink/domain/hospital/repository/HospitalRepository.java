package com.dentallink.domain.hospital.repository;

import com.dentallink.domain.hospital.dto.response.HospitalDetailResponse;
import com.dentallink.domain.hospital.dto.response.HospitalListResponse;
import com.dentallink.domain.hospital.entity.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * 병원 이름으로 검색 (채팅봇용)
     * - 정확도 순, Pageable로 제한
     * - 데이터베이스 독립적인 쿼리 (LIMIT 대신 Pageable 사용)
     */
    @Query("""
    SELECT h FROM Hospital h
    WHERE h.deleted IS false
    AND h.hospitalName LIKE concat('%', :keyword, '%')
    ORDER BY h.hospitalName ASC
    """)
    Page<Hospital> searchHospitalsByName(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 병원 주소로 검색 (채팅봇용)
     * - 정확도 순, Pageable로 제한
     * - 데이터베이스 독립적인 쿼리 (LIMIT 대신 Pageable 사용)
     */
    @Query("""
    SELECT h FROM Hospital h
    WHERE h.deleted IS false
    AND h.hospitalAddress IS NOT NULL
    AND h.hospitalAddress LIKE concat('%', :location, '%')
    ORDER BY h.hospitalAddress ASC
    """)
    Page<Hospital> searchHospitalsByLocation(@Param("location") String location, Pageable pageable);

    /**
     * 의사 이름으로 검색 (채팅봇용)
     * - 정확도 순, Pageable로 제한
     * - 데이터베이스 독립적인 쿼리 (LIMIT 대신 Pageable 사용)
     */
    @Query("""
    SELECT h FROM Hospital h
    WHERE h.deleted IS false
    AND h.doctorName IS NOT NULL
    AND h.doctorName LIKE concat('%', :doctorName, '%')
    ORDER BY h.doctorName ASC
    """)
    Page<Hospital> searchHospitalsByDoctor(@Param("doctorName") String doctorName, Pageable pageable);

}
