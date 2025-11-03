package com.dentallink.domain.paymentLog.service;

import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.paymentLog.entity.PaymentLog;
import com.dentallink.domain.paymentLog.enums.PaymentLogStatus;
import com.dentallink.domain.paymentLog.repository.PaymentLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentLogExternalService {
    private final PaymentLogRepository paymentLogRepository;

    public void createLog(Payment payment, PaymentLogStatus status, String message) {
        PaymentLog log = PaymentLog.create(payment, status, message);
        paymentLogRepository.save(log);
    }

}
