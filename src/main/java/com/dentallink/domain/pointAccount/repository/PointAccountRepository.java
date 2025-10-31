package com.dentallink.domain.pointAccount.repository;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    // 기본 조회 (락 없음)
    Optional<PointAccount> findByUserId(Long userId);

    // 비관적 락 (userId 기준)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PointAccount p WHERE p.user.id = :userId")
    Optional<PointAccount> findByUserIdWithLock(@Param("userId") Long userId);

    // 비관적 락 (accountId 기준)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PointAccount p WHERE p.id = :id")
    Optional<PointAccount> findByIdWithLock(@Param("id") Long id);
}
