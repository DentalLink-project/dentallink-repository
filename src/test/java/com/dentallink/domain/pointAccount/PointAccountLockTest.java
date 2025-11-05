package com.dentallink.domain.pointAccount;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.enums.UserRole;
import com.dentallink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointAccountLockTest {

    @Autowired
    private PointAccountExternalService pointAccountExternalService;

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Autowired
    private UserRepository userRepository;

    private Long accountId;

    @BeforeEach
    void setUp() {
        // 테스트용 유저 생성 및 저장
        User user = User.of(
                "someone@example.com",
                "encodedPassword",   // 실제 서비스에서는 BCryptEncoder로 암호화된 값
                "홍길동",
                UserRole.ROLE_USER
        );
        userRepository.save(user);

        // 유저 연결된 포인트 계좌 생성
        PointAccount account = PointAccount.create(user, 0L);
        pointAccountRepository.save(account);
        accountId = account.getId();
    }

    @Test
    @DisplayName("3명이 동시에 10000원씩 입금해도 락이 걸려 최종 잔액은 30000원이어야 한다")
    void concurrentDepositTest() throws InterruptedException {
        int threadCount = 3;
        long depositAmount = 10_000L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 동시에 3명이 입금 시도
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    pointAccountExternalService.depositPointAccount(accountId, depositAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // DB에서 결과 확인
        PointAccount result = pointAccountRepository.findById(accountId)
                .orElseThrow();

        System.out.println("최종 잔액 = " + result.getBalance());
        assertThat(result.getBalance()).isEqualTo(30_000L);
    }
}
