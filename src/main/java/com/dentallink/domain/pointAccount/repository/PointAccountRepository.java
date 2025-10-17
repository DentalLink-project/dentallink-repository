package com.dentallink.domain.pointAccount.repository;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointAccountRepository extends JpaRepository<PointAccount,Long> {
}
