package com.dentallink.domain.pointAccount.entity;

import com.dentallink.common.entity.BaseEntity;
import com.dentallink.domain.user.entity.User;
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

    // 유저와 포인트계좌는 1대1 관계이다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private PointAccount(User user, Long balance) {
        this.user = user;
        this.balance = balance;
    }

    public static PointAccount create(User user, Long balance) {
        return new PointAccount(user, balance);
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
