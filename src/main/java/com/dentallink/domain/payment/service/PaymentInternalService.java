package com.dentallink.domain.payment.service;

import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.payment.enums.PaymentMethod;
import com.dentallink.domain.payment.repository.PaymentRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.user.entity.User;
import com.dentallink.domain.user.service.UserExternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentInternalService {
    private final PaymentRepository paymentRepository;
    private final PointAccountExternalService pointAccountExternalService;
    private final UserExternalService userQueryService;

    @Transactional
    public PaymentResponse depositPoint(Long userId, Long amount, PaymentMethod paymentMethod) {
        User user = userQueryService.getUserById(userId);
        PointAccount account = pointAccountExternalService.getPointAccountByUser(user);
        Payment payment = Payment.create(account, amount, paymentMethod);
        payment.markSuccess();
        paymentRepository.save(payment);
        pointAccountExternalService.depositPointAccount(account.getId(), amount);
        return PaymentResponse.from(payment);
    }

}


