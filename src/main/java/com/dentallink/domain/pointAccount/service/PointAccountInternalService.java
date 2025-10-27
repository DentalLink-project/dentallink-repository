package com.dentallink.domain.pointAccount.service;

import com.dentallink.domain.pointAccount.dto.request.PointAccountWithdrawRequest;
import com.dentallink.domain.pointAccount.dto.response.*;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.exception.InvalidPointAccountException;
import com.dentallink.domain.pointAccount.exception.PointAccountErrorCode;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.service.PointLogExternalService;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.service.UserExternalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccountInternalService {
    public final PointAccountRepository pointAccountRepository;
    private final PointLogExternalService pointLogExternalService;
    private final UserExternalService userExternalService;

    // 계좌 생성하기
    @Transactional
    public PointAccountCreateResponse createPointAccount(Long userId) {
        User user = userExternalService.getUserById(userId);
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
        User user = userExternalService.getUserById(userId);
        PointAccount account = pointAccountRepository.findByUser(user)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        return PointAccountGetResponse.from(account);
    }

    // 관계자가 특정 포인트 계좌에 포인트를 충전하는 메서드
    @Transactional
    public PointAccountDepositResponse depositPointAccount(Long accountId, Long amount){
        PointAccount account = pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.deposit(amount);
        // 로그 생성
        pointLogExternalService.createLog(account, PointLogType.DEPOSIT, amount);
        return PointAccountDepositResponse.from(account, amount);
    }

    @Transactional
    public PointAccountWithdrawResponse withdrawPointAccount(Long userId, PointAccountWithdrawRequest request) {
        User user = userExternalService.getUserById(userId);
        PointAccount account = pointAccountRepository.findByUser(user)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
        account.withdraw(request.amount());
        pointLogExternalService.createLog(account, PointLogType.WITHDRAW, request.amount());

        return PointAccountWithdrawResponse.from(account, request);
    }


}
