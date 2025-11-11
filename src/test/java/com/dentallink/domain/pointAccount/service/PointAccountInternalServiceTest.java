package com.dentallink.domain.pointAccount.service;

import com.dentallink.domain.pointAccount.dto.request.PointAccountWithdrawRequest;
import com.dentallink.domain.pointAccount.dto.response.*;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.enums.PointAccountType;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
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
class PointAccountInternalServiceTest {

    @Autowired
    private PointAccountInternalService pointAccountInternalService;

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointLogRepository pointLogRepository;

    private Long userId;
    private Long accountId;

    @BeforeEach
    void setUp() {
        // 테스트 유저 생성
        User user = User.of(
                "testuser@example.com",
                "encodedPassword",
                "홍길동",
                UserRole.ROLE_USER
        );
        userRepository.save(user);
        userId = user.getId();

        // 포인트 계좌 생성 및 저장
        PointAccount account = PointAccount.create(user, 0L);
        pointAccountRepository.save(account);
        accountId = account.getId();
    }

    @Test
    @DisplayName("포인트 충전 성공 및 로그 생성")
    void depositPointAccount_success() {
        Long amount = 5000L;

        System.out.println("\n=== 포인트 충전 테스트 시작 ===");
        System.out.println("충전 요청 금액: " + amount + "원");

        PointAccountDepositResponse response =
                pointAccountInternalService.depositPointAccount(accountId, amount);

        System.out.println("충전 후 잔액: " + response.balance() + "원");

        assertThat(response.type()).isEqualTo(PointAccountType.DEPOSIT);
        assertThat(response.amount()).isEqualTo(amount);
        assertThat(response.balance()).isEqualTo(5000L);

        PointAccount updated = pointAccountRepository.findById(accountId).orElseThrow();
        assertThat(updated.getBalance()).isEqualTo(5000L);

        System.out.println("DB 반영 잔액 확인: " + updated.getBalance() + "원");
        assertThat(pointLogRepository.findAll()).hasSize(1);
        assertThat(pointLogRepository.findAll().get(0).getType().name()).isEqualTo("DEPOSIT");

        System.out.println("로그 생성 완료 (DEPOSIT)");
        System.out.println("=== 포인트 충전 테스트 성공 ===\n");
    }

    @Test
    @DisplayName("포인트 출금 성공 및 로그 생성")
    void withdrawPointAccount_success() {
        System.out.println("\n=== 포인트 출금 테스트 시작 ===");
        pointAccountInternalService.depositPointAccount(accountId, 10000L);
        System.out.println("초기 잔액: 10000원 (테스트용 충전 완료)");

        PointAccountWithdrawRequest request = new PointAccountWithdrawRequest(
                3000L,
                "국민은행",
                "123-456-7890",
                "홍길동"
        );

        PointAccountWithdrawResponse response =
                pointAccountInternalService.withdrawPointAccount(userId, request);

        System.out.println("출금 요청 금액: " + response.amount() + "원");
        System.out.println("출금 후 잔액: " + response.balance() + "원");
        System.out.println("출금 계좌 정보: " +
                response.bankName() + " / " +
                response.accountNumber() + " / " +
                response.accountHolder());

        assertThat(response.type()).isEqualTo(PointAccountType.WITHDRAW);
        assertThat(response.amount()).isEqualTo(3000L);
        assertThat(response.balance()).isEqualTo(7000L);

        PointAccount updated = pointAccountRepository.findById(accountId).orElseThrow();
        assertThat(updated.getBalance()).isEqualTo(7000L);
        System.out.println("DB 반영 잔액 확인: " + updated.getBalance() + "원");

        assertThat(pointLogRepository.findAll()).hasSize(2);
        assertThat(pointLogRepository.findAll().get(1).getType().name()).isEqualTo("WITHDRAW");

        System.out.println("로그 생성 완료 (WITHDRAW)");
        System.out.println("=== 포인트 출금 테스트 성공 ===\n");
    }

    @Test
    @DisplayName("포인트 계좌 조회 성공")
    void getPointAccount_success() {
        System.out.println("\n=== 포인트 계좌 조회 테스트 시작 ===");
        PointAccountGetResponse response = pointAccountInternalService.getPointAccount(userId);

        System.out.println("조회된 잔액: " + response.balance() + "원");
        assertThat(response.balance()).isEqualTo(0L);
        System.out.println("테스트 통과: 포인트 계좌 조회 성공!\n");
    }
}


