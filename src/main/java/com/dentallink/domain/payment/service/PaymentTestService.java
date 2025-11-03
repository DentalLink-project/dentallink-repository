package com.dentallink.domain.payment.service;

import com.dentallink.domain.payment.dto.request.PaymentConfirmRequest;
import com.dentallink.domain.payment.dto.request.PaymentReadyRequest;
import com.dentallink.domain.payment.dto.response.PaymentCancelResponse;
import com.dentallink.domain.payment.dto.response.PaymentReadyResponse;
import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.payment.enums.PaymentStatus;
import com.dentallink.domain.payment.exception.InvalidPaymentException;
import com.dentallink.domain.payment.exception.PaymentErrorCode;
import com.dentallink.domain.payment.repository.PaymentRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService ;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.service.PointLogExternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PaymentTestService{

    private final PointAccountExternalService pointAccountExternalService;
    private final PaymentRepository paymentRepository;
    private final PointLogExternalService pointLogExternalService;

    @Transactional
    public PaymentReadyResponse testReadyPayment(Long userId, PaymentReadyRequest request) {
        PointAccount account = pointAccountExternalService.getPointAccountByUserId(userId);

        // 이미 같은 orderId가 존재하면 중복 에러 방지
        paymentRepository.findByOrderId(request.orderId())
                .ifPresent(p -> {
                    throw new InvalidPaymentException(PaymentErrorCode.DUPLICATE_ORDER_ID);
                });

        Payment payment = Payment.create(account, request.amount(), request.orderId());
        Payment saved = paymentRepository.save(payment);
        System.out.println("결제 준비 완료: " + saved.getOrderId());
        return PaymentReadyResponse.from(saved);
    }

    @Transactional
    public PaymentCancelResponse testCancelPayment(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvalidPaymentException(PaymentErrorCode.NOT_FOUND_ORDER_ID));

        if (payment.getStatus() != PaymentStatus.READY) {
            throw new InvalidPaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
        }
        payment.markCancelled();
        System.out.println("결제 취소 완료: " + orderId);
        return PaymentCancelResponse.from(payment);
    }


    @Transactional
    public PaymentResponse testConfirmPayment(Long userId, PaymentConfirmRequest request) {
        PointAccount account = pointAccountExternalService.getPointAccountByUserId(userId);

        Payment payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new InvalidPaymentException(PaymentErrorCode.NOT_FOUND_ORDER_ID));

        if (!payment.getAmount().equals(request.amount())) {
            throw new InvalidPaymentException(PaymentErrorCode.INVALID_AMOUNT);
        }

        if (payment.getStatus() != PaymentStatus.READY) {
            throw new InvalidPaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 결제 성공 처리
        account.deposit(request.amount());
        payment.markSuccess(request.paymentKey());
        pointLogExternalService.createLog(account, PointLogType.DEPOSIT, request.amount());

        System.out.println("결제 승인 완료: " + request.orderId());
        return PaymentResponse.from(payment);
    }
}
