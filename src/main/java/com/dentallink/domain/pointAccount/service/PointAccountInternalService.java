package com.dentallink.domain.pointAccount.service;

import com.dentallink.domain.pointAccount.dto.response.*;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.exception.InvalidPointAccountException;
import com.dentallink.domain.pointAccount.exception.PointAccountErrorCode;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.service.query.UserQueryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccountInternalService {
    public final PointAccountRepository pointAccountRepository;
    private final UserQueryService userQueryService;

    // 계좌 생성하기
    @Transactional
    public PointAccountCreateResponse createPointAccount(Long userId) {
        User user = userQueryService.getUserById(userId);
        if (pointAccountRepository.findByUser(user).isPresent()) {
            throw new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
        PointAccount account = PointAccount.create(user, 0L);
        pointAccountRepository.save(account);
        return PointAccountCreateResponse.from(account);
    }

    // 계좌 잔액 확인하기
    @Transactional(readOnly = true)
    public PointAccountGetResponse getPointAccount(Long userId) {
        User user = userQueryService.getUserById(userId);
        PointAccount account = pointAccountRepository.findByUser(user)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        return PointAccountGetResponse.from(account);
    }

    /*
    // 현금을 포인트로 바꾸는 메서드
    @Transactional
    public PointAccountDepositResponse depositPointAccount(Long accountId, Long amount){
        PointAccount account = pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.deposit(amount);
        // 로그 생성
        pointLogExternalService.createLog(account, PointLogType.DEPOSIT, amount);
        return PointAccountDepositResponse.from(account, amount);
    }

    // 포인트를 현금으로 바꾸는 메서드(payment 도메인 사용)
    @Transactional
    public PointAccountWithdrawResponse withdrawPointAccount(Long accountId, Long amount){
        PointAccount account = pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.withdraw(amount);
        pointLogExternalService.createLog(account, PointLogType.WITHDRAW, amount);
        return PointAccountWithdrawResponse.from(account, amount);
    }

    // 포인트를 통해 상품 구매(reservation 도메인에서 사용)
    @Transactional
    public PointAccountSpendResponse spendPointAccount(Long accountId, Long amount){
        PointAccount account = pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.spend(amount);
        pointLogExternalService.createLog(account, PointLogType.SPEND, amount);
        return PointAccountSpendResponse.from(account, amount);
    }

    // 상품 구매 취소를 통해 포인트 복구(reservation 도메인에서 사용)
    @Transactional
    public PointAccountRefundResponse refundPointAccount(Long accountId, Long amount){
        PointAccount account = pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.refund(amount);
        pointLogExternalService.createLog(account, PointLogType.REFUND, amount);
        return PointAccountRefundResponse.from(account, amount);
    }
     */
}
