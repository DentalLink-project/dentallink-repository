package com.dentallink.domain.pointLog.repository;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointLog.entity.PointLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PointLogRepository extends JpaRepository<PointLog,Long> {
    Page<PointLog> findByPointAccount(PointAccount account, Pageable pageable);

    Page<PointLog> findByPointAccountAndCreatedAtBetween(
            PointAccount account,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
}
