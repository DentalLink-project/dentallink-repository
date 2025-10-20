package com.dentallink.domain.pointLog.repository;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointLog.entity.PointLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLogRepository extends JpaRepository<PointLog,Long> {
    Page<PointLog> findByPointAccount(PointAccount account, Pageable pageable);
}
