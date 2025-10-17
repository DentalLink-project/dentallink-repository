package com.dentallink.domain.pointLog.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointLog.enums.PointLogType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 포인트계좌와 포인트 로그는 1:N
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_account_id", nullable = false)
    private PointAccount pointAccount;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointLogType type;

    // 거래 후 잔액
    @Column(nullable = false)
    private Long balanceAfter;

    private PointLog(PointAccount account, PointLogType type, Long amount, Long balanceAfter) {
        this.pointAccount = account;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public static PointLog create(PointAccount account, PointLogType type, Long amount) {
        return new PointLog(account, type, amount, account.getBalance());
    }
}
