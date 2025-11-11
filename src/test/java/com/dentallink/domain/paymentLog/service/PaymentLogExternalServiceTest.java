package com.dentallink.domain.paymentLog.service;

import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.payment.enums.PaymentStatus;
import com.dentallink.domain.payment.repository.PaymentRepository;
import com.dentallink.domain.paymentLog.entity.PaymentLog;
import com.dentallink.domain.paymentLog.enums.PaymentLogStatus;
import com.dentallink.domain.paymentLog.repository.PaymentLogRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
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
class PaymentLogExternalServiceTest {

    @Autowired
    private PaymentLogExternalService paymentLogExternalService;

    @Autowired
    private PaymentLogRepository paymentLogRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PointAccountRepository pointAccountRepository;

    @Autowired
    private UserRepository userRepository;

    private Payment payment;

    @BeforeEach
    void setUp() {
        // 테스트용 유저 생성
        User user = User.of(
                "paymentlog@test.com",
                "encodedPassword",
                "홍길동",
                UserRole.ROLE_USER
        );
        userRepository.save(user);

        // 포인트 계좌 생성
        PointAccount account = PointAccount.create(user, 0L);
        pointAccountRepository.save(account);

        // 결제 엔티티 생성 및 저장
        payment = Payment.create(account, 10000L, "ORDER-LOG-TEST");
        paymentRepository.save(payment);

        System.out.println("테스트 준비: userId=" + user.getId() + ", paymentId=" + payment.getId() + "\n");
    }

    @Test
    @DisplayName("결제 로그 생성 성공 (READY)")
    void createLog_success_ready() {
        // when
        paymentLogExternalService.createLog(payment, PaymentLogStatus.READY, "결제 준비 완료");

        // then
        List<PaymentLog> logs = paymentLogRepository.findAll();
        assertThat(logs).hasSize(1);
        PaymentLog log = logs.get(0);

        assertThat(log.getStatus()).isEqualTo(PaymentLogStatus.READY);
        assertThat(log.getMessage()).isEqualTo("결제 준비 완료");
        assertThat(log.getPayment().getOrderId()).isEqualTo("ORDER-LOG-TEST");

        System.out.println("로그 생성 성공: " + log.getStatus() + " | message=" + log.getMessage() + " | paymentId=" + payment.getId() + "\n");
    }

    @Test
    @DisplayName("결제 승인 성공 로그 생성")
    void createLog_success_paymentApproved() {
        // given
        payment.markSuccess("PAYMENT_KEY_123");

        // when
        paymentLogExternalService.createLog(payment, PaymentLogStatus.SUCCESS, "결제 승인 완료");

        // then
        List<PaymentLog> logs = paymentLogRepository.findAll();
        assertThat(logs).hasSize(1);
        PaymentLog log = logs.get(0);

        assertThat(log.getStatus()).isEqualTo(PaymentLogStatus.SUCCESS);
        assertThat(log.getPayment().getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        System.out.println("결제 승인 로그 기록 완료: " + log.getStatus() + " | paymentKey=" + payment.getPaymentKey() + " | message=" + log.getMessage() + "\n");
    }

    @Test
    @DisplayName("결제 실패 로그 생성")
    void createLog_failedPayment() {
        // given
        payment.markFailed();

        // when
        paymentLogExternalService.createLog(payment, PaymentLogStatus.FAILED, "결제 승인 실패 (금액 불일치)");

        // then
        List<PaymentLog> logs = paymentLogRepository.findAll();
        assertThat(logs).hasSize(1);
        PaymentLog log = logs.get(0);

        assertThat(log.getStatus()).isEqualTo(PaymentLogStatus.FAILED);
        assertThat(log.getMessage()).contains("실패");
        assertThat(log.getPayment().getStatus()).isEqualTo(PaymentStatus.FAILED);

        System.out.println("결제 실패 로그 기록 완료: " + log.getStatus() + " | message=" + log.getMessage());
    }

    @Test
    @DisplayName("결제 취소 로그 생성")
    void createLog_cancelledPayment() {
        // given
        payment.markCancelled();

        // when
        paymentLogExternalService.createLog(payment, PaymentLogStatus.CANCELLED, "결제 취소 완료");

        // then
        List<PaymentLog> logs = paymentLogRepository.findAll();
        assertThat(logs).hasSize(1);
        PaymentLog log = logs.get(0);

        assertThat(log.getStatus()).isEqualTo(PaymentLogStatus.CANCELLED);
        assertThat(log.getMessage()).isEqualTo("결제 취소 완료");
        assertThat(log.getPayment().getStatus()).isEqualTo(PaymentStatus.CANCELLED);

        System.out.println("결제 취소 로그 기록 완료: " + log.getStatus() + " | message=" + log.getMessage());
    }
}
