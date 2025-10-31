package com.dentallink.domain.payment.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.payment.enums.PaymentMethod;
import com.dentallink.domain.payment.enums.PaymentStatus;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_account_id", nullable = false)
    private PointAccount pointAccount;

    @Column(nullable = false)
    private Long amount;

    // 결제 상태 (READY, SUCCESS, FAILED, CANCELLED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // 결제 수단 (TOSS, CARD, CASH, KAKAO, NAVER 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = true) // ✅ Toss 결제 시 초기에는 null 가능
    private PaymentMethod method;

    // 주문
    @Column(unique = true, nullable = false)
    private String orderId; // 프론트에서 생성한 고유 주문 ID

    @Column(unique = true)
    private String paymentKey; // Toss 서버에서 반환한 결제 고유 키

    // 타임스탬프
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;


    // 일반 결제
    private Payment(PointAccount pointAccount, Long amount, PaymentMethod method, String orderId) {
        this.pointAccount = pointAccount;
        this.amount = amount;
        this.method = method;
        this.orderId = orderId;
        this.status = PaymentStatus.READY;
        this.requestedAt = LocalDateTime.now();
    }

    // Toss 결제 (method 없이 생성)
    private Payment(PointAccount pointAccount, Long amount, String orderId) {
        this.pointAccount = pointAccount;
        this.amount = amount;
        this.orderId = orderId;
        this.status = PaymentStatus.READY;
        this.requestedAt = LocalDateTime.now();
    }

    // 일반 결제 생성 (내부 결제, 포인트 등)
    public static Payment create(PointAccount account, Long amount, PaymentMethod method, String orderId) {
        return new Payment(account, amount, method, orderId);
    }

    // Toss 결제용 (method 아직 모름)
    public static Payment create(PointAccount account, Long amount, String orderId) {
        return new Payment(account, amount, orderId);
    }


    // 결제 수단
    public void assignMethod(PaymentMethod method) {
        this.method = method;
    }

    // 결제 승인 성공
    public void markSuccess(String paymentKey) {
        this.status = PaymentStatus.SUCCESS;
        this.paymentKey = paymentKey;
        this.approvedAt = LocalDateTime.now();
    }

    // 결제 실패
    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    // 결제 취소
    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }
}
