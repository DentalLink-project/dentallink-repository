package com.dentallink.domain.pointAccount.entity;

import com.dentallink.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long balance;

    private PointAccount(Long balance) {
        this.balance = balance;
    }

    public static PointAccount create(Long balance) {
        return new PointAccount(balance);
    }

    // 현금 -> 포인트
    public void deposit(Long amount) {
        validatePositive(amount);
        this.balance += amount;
    }

    // 포인트 -> 현금
    public void withdraw(Long amount) {
        validatePositive(amount);
        if (this.balance < amount) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.balance -= amount;
    }

    // 포인트 -> 상품 구매
    public void spend(Long amount) {
        validatePositive(amount);
        if (this.balance < amount) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.balance -= amount;
    }

    // 상품 구매 취소 -> 포인트 복구
    public void refund(Long amount) {
        validatePositive(amount);
        if (this.balance < amount) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.balance += amount;
    }

    private void validatePositive(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
    }
}
