package com.dentallink.domain.pointLog.repository;

import com.dentallink.domain.pointLog.entity.PointLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLogRepository extends JpaRepository<PointLog,Long> {
}
