package com.dentallink.domain.payment.service;

import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.payment.repository.PaymentRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.service.PointLogExternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentInternalService {
    private final PaymentRepository paymentRepository;
    private final PointAccountExternalService pointAccountExternalService;
    private final PointLogExternalService pointLogExternalService;

    @Transactional
    public PaymentResponse depositPoint(Long accountId, Long amount) {
        PointAccount account = pointAccountExternalService.getPointAccountById(accountId);

        account.deposit(amount);
        pointLogExternalService.createLog(account, PointLogType.DEPOSIT, amount);
        Payment payment = Payment.create(account, amount);
        paymentRepository.save(payment);
        return PaymentResponse.from(payment);
    }
}
