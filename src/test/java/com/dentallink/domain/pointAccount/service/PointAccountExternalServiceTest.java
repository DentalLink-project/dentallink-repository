package com.dentallink.domain.pointAccount.service;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PointAccountExternalServiceTest {

    @Autowired
    private PointAccountExternalService pointAccountExternalService;

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Autowired
    private PointLogRepository pointLogRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private Long accountId;

    @BeforeEach
    void setUp() {
        // 유저 생성
        User user = User.of(
                "tester@example.com",
                "encodedPassword",
                "홍길동",
                UserRole.ROLE_USER
        );
        userRepository.save(user);
        userId = user.getId();

        // 포인트 계좌 생성
        PointAccount account = PointAccount.create(user, 0L);
        pointAccountRepository.save(account);
        accountId = account.getId();

        System.out.println("테스트 초기화: userId=" + userId + ", accountId=" + accountId + "\n");
    }

    @Test
    @DisplayName("포인트 충전 성공 및 로그 생성")
    void depositPointAccount_success() {
        // when
        pointAccountExternalService.depositPointAccount(accountId, 5000L);

        // then
        PointAccount updated = pointAccountRepository.findById(accountId).orElseThrow();
        assertThat(updated.getBalance()).isEqualTo(5000L);
        assertThat(pointLogRepository.findAll()).hasSize(1);
        assertThat(pointLogRepository.findAll().get(0).getType()).isEqualTo(PointLogType.DEPOSIT);

        System.out.println("충전 완료: +5000원 \n 현재 잔액 = " + updated.getBalance() + "원\n");
    }

    @Test
    @DisplayName("포인트 사용(지출) 성공 및 로그 생성")
    void spendPointAccount_success() {
        // given
        pointAccountExternalService.depositPointAccount(accountId, 10000L);

        // when
        pointAccountExternalService.spendPointAccount(accountId, 3000L);

        // then
        PointAccount updated = pointAccountRepository.findById(accountId).orElseThrow();
        assertThat(updated.getBalance()).isEqualTo(7000L);

        assertThat(pointLogRepository.findAll()).hasSize(2);
        assertThat(pointLogRepository.findAll().get(1).getType()).isEqualTo(PointLogType.SPEND);

        System.out.println("지출 완료: -3000원 \n 현재 잔액 = " + updated.getBalance() + "원\n");
    }

    @Test
    @DisplayName("포인트 환불 성공 및 로그 생성")
    void refundPointAccount_success() {
        // given
        pointAccountExternalService.depositPointAccount(accountId, 10000L);
        pointAccountExternalService.spendPointAccount(accountId, 3000L);

        // when
        pointAccountExternalService.refundPointAccount(accountId, 3000L);

        // then
        PointAccount updated = pointAccountRepository.findById(accountId).orElseThrow();
        assertThat(updated.getBalance()).isEqualTo(10000L);

        assertThat(pointLogRepository.findAll()).hasSize(3);
        assertThat(pointLogRepository.findAll().get(2).getType()).isEqualTo(PointLogType.REFUND);

        System.out.println("환불 완료: +3000원 \n 현재 잔액 = " + updated.getBalance() + "원\n");
    }

    @Test
    @DisplayName("유저 최초 포인트 계좌 생성 성공")
    void createPointAccount_success() {
        // given
        User newUser = User.of(
                "newuser@example.com",
                "encodedPassword",
                "이순신",
                UserRole.ROLE_USER
        );
        userRepository.save(newUser);

        // when
        pointAccountExternalService.createPointAccount(newUser);

        // then
        PointAccount created = pointAccountRepository.findByUserId(newUser.getId()).orElseThrow();
        assertThat(created.getBalance()).isEqualTo(0L);

        System.out.println("신규 계좌 생성 완료 → userId=" + newUser.getId() +
                ", accountId=" + created.getId() +
                ", 초기 잔액=" + created.getBalance() + "원\n");
    }

    @Test
    @DisplayName("유저 ID로 포인트 계좌 조회 성공")
    void getPointAccountByUserId_success() {
        // when
        PointAccount found = pointAccountExternalService.getPointAccountByUserId(userId);

        // then
        assertThat(found.getId()).isEqualTo(accountId);
        assertThat(found.getBalance()).isEqualTo(0L);

        System.out.println("계좌 조회 완료 → userId=" + userId +
                ", accountId=" + found.getId() +
                ", 잔액=" + found.getBalance() + "원");
    }
}
