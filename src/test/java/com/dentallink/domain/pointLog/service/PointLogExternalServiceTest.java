package com.dentallink.domain.pointLog.service;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.pointLog.entity.PointLog;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.repository.PointLogRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PointLogExternalServiceTest {

    @Autowired
    private PointLogExternalService pointLogExternalService;

    @Autowired
    private PointLogRepository pointLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointAccountRepository pointAccountRepository;

    private PointAccount account;

    @BeforeEach
    void setUp() {
        // 테스트용 유저 및 포인트 계좌 생성
        User user = User.of(
                "pointlogtest@example.com",
                "encodedPassword",
                "홍길동",
                UserRole.ROLE_USER
        );
        userRepository.save(user);

        account = PointAccount.create(user, 1000L);
        pointAccountRepository.save(account);

        System.out.println("테스트 준비 완료 → userId=" + user.getId() + ", accountId=" + account.getId());
    }

    @Test
    @DisplayName(" 포인트 적립(DEPOSIT) 로그 생성 성공")
    void createDepositLog_success() {
        // when
        pointLogExternalService.createLog(account, PointLogType.DEPOSIT, 500L);

        // then
        List<PointLog> logs = pointLogRepository.findAll();
        assertThat(logs).hasSize(1);

        PointLog log = logs.get(0);
        assertThat(log.getType()).isEqualTo(PointLogType.DEPOSIT);
        assertThat(log.getAmount()).isEqualTo(500L);

        System.out.println("💰 [DEPOSIT] 로그 생성 완료 → amount=" + log.getAmount());
    }

    @Test
    @DisplayName("포인트 출금(WITHDRAW) 로그 생성 성공")
    void createWithdrawLog_success() {
        // when
        pointLogExternalService.createLog(account, PointLogType.WITHDRAW, 300L);

        // then
        PointLog log = pointLogRepository.findAll().get(0);
        assertThat(log.getType()).isEqualTo(PointLogType.WITHDRAW);
        assertThat(log.getAmount()).isEqualTo(300L);

        System.out.println("[WITHDRAW] 로그 생성 완료 → amount=" + log.getAmount());
    }

    @Test
    @DisplayName("포인트 사용(SPEND) 로그 생성 성공")
    void createSpendLog_success() {
        // when
        pointLogExternalService.createLog(account, PointLogType.SPEND, 200L);

        // then
        PointLog log = pointLogRepository.findAll().get(0);
        assertThat(log.getType()).isEqualTo(PointLogType.SPEND);
        assertThat(log.getAmount()).isEqualTo(200L);

        System.out.println(" [SPEND] 로그 생성 완료 → amount=" + log.getAmount());
    }

    @Test
    @DisplayName("포인트 환불(REFUND) 로그 생성 성공")
    void createRefundLog_success() {
        // when
        pointLogExternalService.createLog(account, PointLogType.REFUND, 150L);

        // then
        PointLog log = pointLogRepository.findAll().get(0);
        assertThat(log.getType()).isEqualTo(PointLogType.REFUND);
        assertThat(log.getAmount()).isEqualTo(150L);

        System.out.println("[REFUND] 로그 생성 완료 → amount=" + log.getAmount());
    }
}
