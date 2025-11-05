package com.dentallink.common.monitering;

import com.dentallink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private final UserRepository userRepository;

    @Override
    public Health health() {
        boolean dbUp = checkDatabaseConnection();
        if (dbUp) {
            return Health.up().withDetail("database", "OK").build();
        }
        return Health.down().withDetail("database", "DOWN").build();
    }

    private boolean checkDatabaseConnection() {
        long count = userRepository.count();
        return userRepository.count() >= 0; // 실제 DB 연결 확인 로직으로 대체
    }
}
