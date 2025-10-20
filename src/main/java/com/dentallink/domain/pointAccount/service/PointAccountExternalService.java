package com.dentallink.domain.pointAccount.service;

import com.dentallink.domain.pointAccount.dto.response.*;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.exception.InvalidPointAccountException;
import com.dentallink.domain.pointAccount.exception.PointAccountErrorCode;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.service.PointLogExternalService;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointAccountExternalService {
    private final PointAccountRepository pointAccountRepository;
    private final UserQueryService userQueryService;
    private final PointLogExternalService pointLogExternalService;

    // pointAccountId 찾는 메서드
    @Transactional(readOnly = true)
    public PointAccount getPointAccountById(Long accountId) {
        return pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PointAccount getPointAccountByUser(User user) {
        return pointAccountRepository.findByUser(user)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
    }
    // 현금을 포인트로 바꾸는 메서드
    @Transactional
    public void depositPointAccount(Long accountId, Long amount){
        PointAccount account = pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.deposit(amount);
        // 로그 생성
        pointLogExternalService.createLog(account, PointLogType.DEPOSIT, amount);
    }

    // 포인트를 현금으로 바꾸는 메서드(payment 도메인 사용)
    @Transactional
    public void withdrawPointAccount(Long accountId, Long amount){
        PointAccount account = pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.withdraw(amount);
        pointLogExternalService.createLog(account, PointLogType.WITHDRAW, amount);
    }

    // 포인트를 통해 상품 구매(reservation 도메인에서 사용)
    @Transactional
    public void spendPointAccount(Long accountId, Long amount){
        PointAccount account = pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.spend(amount);
        pointLogExternalService.createLog(account, PointLogType.SPEND, amount);
    }

    // 상품 구매 취소를 통해 포인트 복구(reservation 도메인에서 사용)
    @Transactional
    public void refundPointAccount(Long accountId, Long amount){
        PointAccount account = pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.refund(amount);
        pointLogExternalService.createLog(account, PointLogType.REFUND, amount);
    }

    // 계좌 잔액을 보여주기
    @Transactional(readOnly = true)
    public PointAccountGetResponse getPointAccount(Long userId) {
        User user = userQueryService.getUserById(userId);
        PointAccount account = pointAccountRepository.findByUser(user)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        return PointAccountGetResponse.from(account);
    }

    // 계좌 잔액을 반환하기
    @Transactional(readOnly = true)
    public Long getPointAccountBalance(Long userId) {
        User user = userQueryService.getUserById(userId);
        PointAccount account = pointAccountRepository.findByUser(user)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        return account.getBalance();
    }
}
