package com.dentallink.domain.paymentLog.service;

import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.payment.repository.PaymentRepository;
import com.dentallink.domain.paymentLog.dto.response.PaymentLogResponse;
import com.dentallink.domain.paymentLog.repository.PaymentLogRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentLogInternalService {

    private final PaymentLogRepository paymentLogRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PageResponse<PaymentLogResponse> getLogsByOrderId(String orderId, int page, int size, String sort) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 orderId의 결제를 찾을 수 없습니다: " + orderId));

        Sort order = "oldest".equalsIgnoreCase(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, order);
        Page<PaymentLogResponse> logs = paymentLogRepository.findByPayment(payment, pageable)
                .map(PaymentLogResponse::from);
        return PageResponse.fromPage(logs);
    }
}
