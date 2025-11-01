package com.dentallink.domain.payment.service;

import com.dentallink.domain.payment.dto.request.PaymentConfirmRequest;
import com.dentallink.domain.payment.dto.request.PaymentReadyRequest;
import com.dentallink.domain.payment.dto.response.PaymentCancelResponse;
import com.dentallink.domain.payment.dto.response.PaymentReadyResponse;
import com.dentallink.domain.payment.dto.response.PaymentResponse;
import com.dentallink.domain.payment.entity.Payment;
import com.dentallink.domain.payment.enums.PaymentMethod;
import com.dentallink.domain.payment.enums.PaymentStatus;
import com.dentallink.domain.payment.exception.InvalidPaymentException;
import com.dentallink.domain.payment.exception.PaymentErrorCode;
import com.dentallink.domain.payment.repository.PaymentRepository;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.service.PointAccountExternalService;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.service.PointLogExternalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PaymentService{

    private final PaymentRepository paymentRepository;
    private final PointAccountExternalService pointAccountExternalService;
    private final PointLogExternalService pointLogExternalService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${toss.secret-key}")
    private String secretKey;

    private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    // [1] 결제 준비 (READY)
    @Transactional
    public PaymentReadyResponse saveReadyPayment(Long userId, PaymentReadyRequest request) {
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

    // [2] 결제 승인 (CONFIRM)
    @Transactional
    public PaymentResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        // 유저 포인트 계좌 조회
        PointAccount account = pointAccountExternalService.getPointAccountByUserId(userId);

        // 결제 조회
        Payment payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new InvalidPaymentException(PaymentErrorCode.NOT_FOUND_ORDER_ID));

        // 금액 검증
        if (!payment.getAmount().equals(request.amount())) {
            throw new InvalidPaymentException(PaymentErrorCode.INVALID_AMOUNT);
        }

        // Toss 승인 요청 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(secretKey, ""); // Basic 인증 자동 Base64 처리

        // 요청 바디
        String requestBody = String.format("""
            {
                "paymentKey": "%s",
                "orderId": "%s",
                "amount": %d
            }
        """, request.paymentKey(), request.orderId(), request.amount());

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Toss API 요청
            ResponseEntity<String> response = restTemplate.exchange(
                    TOSS_CONFIRM_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // 한글 깨짐 방지
            String rawBody = response.getBody();
            if (rawBody == null) {
                throw new InvalidPaymentException(PaymentErrorCode.PAYMENT_RESPONSE_EMPTY);
            }

            String body = new String(rawBody.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            System.out.println("Toss Raw Response = " + body);

            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(body);
            String method = jsonNode.get("method").asText();
            String paymentKey = jsonNode.get("paymentKey").asText();

            // 결제수단 Enum 변환
            payment.assignMethod(PaymentMethod.from(method));

            // 성공 처리
            payment.markSuccess(paymentKey);
            account.deposit(request.amount());
            pointLogExternalService.createLog(account, PointLogType.DEPOSIT, request.amount());

            System.out.println("결제 승인 성공: " + paymentKey);
            return PaymentResponse.from(payment);

        } catch (Exception e) {
            payment.markFailed();
            System.err.println("결제 승인 중 오류 발생: " + e.getMessage());
            throw new InvalidPaymentException(PaymentErrorCode.PAYMENT_APPROVAL_ERROR);
        }
    }

    // [3] 결제 취소 (READY 상태만)
    @Transactional
    public PaymentCancelResponse cancelReadyPayment(Long userId, String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvalidPaymentException(PaymentErrorCode.NOT_FOUND_ORDER_ID));

        if (payment.getStatus() != PaymentStatus.READY) {
            throw new InvalidPaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
        }

        payment.markCancelled();
        System.out.println("결제 취소 완료: " + orderId);
        return PaymentCancelResponse.from(payment);
    }

    //--------------------------------- postman 테스트용-------------------


}
