package com.dentallink.domain.paymentLog.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.paymentLog.enums.PaymentLogStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 올바른 결제 연관관계 (PaymentLog → Payment)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    // 로그 상태 (READY / SUCCESS / FAILED / CANCELLED)
    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private PaymentLogStatus status;

    @Column(nullable = false)
    private Long amount;

    // 로그 메시지 (예: "결제 승인 성공", "결제 실패: Toss API 오류")
    @Column(nullable = false)
    private String message;

    // orderId (결제 트래킹용)
    @Column(nullable = false, unique = false, length = 100)
    private String orderId;

    private PaymentLog(Payment payment, PaymentLogStatus status, Long amount, String message, String orderId) {
        this.payment = payment;
        this.status = status;
        this.amount = amount;
        this.message = message;
        this.orderId = orderId;
    }

    public static PaymentLog create(Payment payment, PaymentLogStatus status, String message) {
        return new PaymentLog(
                payment,
                status,
                payment.getAmount(),
                message,
                payment.getOrderId()
        );
    }
}
