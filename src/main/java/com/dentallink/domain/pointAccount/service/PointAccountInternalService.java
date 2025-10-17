package com.dentallink.domain.pointAccount.service;

import com.dentallink.domain.pointAccount.dto.response.*;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.service.PointLogExternalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccountInternalService {
    public final PointAccountRepository pointAccountRepository;
    private final PointLogExternalService pointLogExternalService;

    // 계좌 생성하기
    @Transactional
    public PointAccountCreateResponse createPointAccount(){
        // 나중에 유저 도메인이 생기면 그를 통해 유저가 이미 계좌가 있는지 확인한다.
        PointAccount account = PointAccount.create(0L);
        pointAccountRepository.save(account);
        return PointAccountCreateResponse.from(account);
    }

    // 현금 -> 포인트
    @Transactional
    public PointAccountDepositResponse depositPointAccount(Long userId, Long amount){
        PointAccount account = pointAccountRepository.findById(userId).orElse(null);
        account.deposit(amount);

        // 로그 생성..
        pointLogExternalService.createLog(account, PointLogType.DEPOSIT, amount);
        return PointAccountDepositResponse.from(account, amount);
    }

    // 포인트 -> 현금
    @Transactional
    public PointAccountWithdrawResponse withdrawPointAccount(Long userId, Long amount){
        PointAccount account = pointAccountRepository.findById(userId).orElse(null);
        account.withdraw(amount);
        pointLogExternalService.createLog(account, PointLogType.WITHDRAW, amount);

        return PointAccountWithdrawResponse.from(account, amount);
    }

    // 포인트 -> 상품 구매
    @Transactional
    public PointAccountSpendResponse spendPointAccount(Long userId, Long amount){
        PointAccount account = pointAccountRepository.findById(userId).orElse(null);
        account.spend(amount);
        pointLogExternalService.createLog(account, PointLogType.SPEND, amount);
        return PointAccountSpendResponse.from(account, amount);
    }

    // 상품 구매 취소 -> 포인트 복구
    @Transactional
    public PointAccountRefundResponse refundPointAccount(Long userId, Long amount){
        PointAccount account = pointAccountRepository.findById(userId).orElse(null);
        account.refund(amount);
        pointLogExternalService.createLog(account, PointLogType.REFUND, amount);
        return PointAccountRefundResponse.from(account, amount);
    }

    // 계좌 잔액 확인하기
    @Transactional
    public PointAccountGetResponse getPointAccount(Long userId){
        PointAccount account = pointAccountRepository.findById(userId).orElse(null);
        return PointAccountGetResponse.from(account);
    }
}
