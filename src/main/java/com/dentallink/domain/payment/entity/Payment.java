package com.dentallink.domain.payment.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_account_id", nullable = false, unique = true)
    private PointAccount pointAccount;

    private Long amount;

    private Payment(PointAccount pointAccount, Long amount) {
        this.pointAccount = pointAccount;
        this.amount = amount;
    }

    public static Payment create(PointAccount pointAccount, Long amount) {
        return new Payment(pointAccount, amount);
    }

}
