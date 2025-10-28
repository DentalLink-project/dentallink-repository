package com.dentallink.domain.pointAccount.repository;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointAccountRepository extends JpaRepository<PointAccount,Long> {
    Optional<PointAccount> findByUserId(Long userId);
}
