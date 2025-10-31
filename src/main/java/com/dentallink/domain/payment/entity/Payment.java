package com.dentallink.domain.payment.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.payment.enums.PaymentMethod;
import com.dentallink.domain.payment.enums.PaymentStatus;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method; // ex) TOSS, CARD, CASH

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status; // ex) READY, SUCCESS, FAILED, CANCELLED

    private Payment(PointAccount pointAccount, Long amount, PaymentMethod method) {
        this.pointAccount = pointAccount;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.READY;
    }

    public static Payment create(PointAccount account, Long amount, PaymentMethod method) {
        return new Payment(account, amount, method);
    }

    public void markSuccess() {
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }


}
