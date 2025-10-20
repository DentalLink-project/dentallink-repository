package com.dentallink.domain.pointAccount.service;

import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.exception.InvalidPointAccountException;
import com.dentallink.domain.pointAccount.exception.PointAccountErrorCode;
import com.dentallink.domain.pointAccount.repository.PointAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointAccountExternalService {
    private final PointAccountRepository pointAccountRepository;

    public PointAccount getPointAccountById(Long accountId) {
        return pointAccountRepository.findById(accountId)
                .orElseThrow(() -> new InvalidPointAccountException(PointAccountErrorCode.ACCOUNT_NOT_FOUND));
    }
}
