package com.dentallink.domain.user.repository;

import com.dentallink.domain.user.dto.response.UserResponse;
import com.dentallink.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedAtIsNull(Long id);
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmail(String email);

    Page<User> findAllWhereDeletedIsFalse(Pageable pageable);
}
