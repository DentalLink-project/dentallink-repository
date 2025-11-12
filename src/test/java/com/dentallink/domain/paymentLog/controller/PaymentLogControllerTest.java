package com.dentallink.domain.paymentLog.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.payment.enums.PaymentMethod;
import com.dentallink.domain.payment.enums.PaymentStatus;
import com.dentallink.domain.paymentLog.dto.response.PaymentLogPaymentResponse;
import com.dentallink.domain.paymentLog.dto.response.PaymentLogResponse;
import com.dentallink.domain.paymentLog.enums.PaymentLogStatus;
import com.dentallink.domain.paymentLog.service.PaymentLogInternalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentLogControllerTest {

    @Mock
    private PaymentLogInternalService paymentLogInternalService;

    @InjectMocks
    private PaymentLogController paymentLogController;

    private PageResponse<PaymentLogResponse> mockPageResponse;

    @BeforeEach
    void setUp() {
        PaymentLogPaymentResponse payment = new PaymentLogPaymentResponse(
                10L,
                PaymentMethod.CARD,
                PaymentStatus.READY,
                5000L
        );

        PaymentLogResponse log1 = new PaymentLogResponse(
                1L, PaymentLogStatus.READY, 5000L, "결제 준비 완료", "ORDER-20251111-AAA", payment
        );
        PaymentLogResponse log2 = new PaymentLogResponse(
                2L, PaymentLogStatus.SUCCESS, 5000L, "결제 성공", "ORDER-20251111-AAA", payment
        );

        mockPageResponse = new PageResponse<>(
                List.of(log1, log2),
                2L,
                1,
                10,
                0
        );
    }

    @Test
    @DisplayName("정상적인 orderId로 결제 로그 조회 성공")
    void getLogsByOrderId_success() {
        // given
        when(paymentLogInternalService.getLogsByOrderId(anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(mockPageResponse);

        // when
        ResponseEntity<CommonApiResponse<PageResponse<PaymentLogResponse>>> response =
                paymentLogController.getLogsByOrderId("ORDER-20251111-AAA", 0, 10, "latest");

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        CommonApiResponse<PageResponse<PaymentLogResponse>> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMessage()).contains("결제 로그 조회 성공");

        PageResponse<PaymentLogResponse> data = body.getData();
        assertThat(data.getContent()).hasSize(2);
        assertThat(data.getContent().get(0).status()).isEqualTo(PaymentLogStatus.READY);
        assertThat(data.getContent().get(1).status()).isEqualTo(PaymentLogStatus.SUCCESS);

        System.out.println("[결제로그조회 성공 테스트]");
        System.out.println("상태코드: " + response.getStatusCode());
        System.out.println("로그 개수: " + data.getContent().size());
        data.getContent().forEach(log -> {
            System.out.println("로그 ID: " + log.id());
            System.out.println("상태: " + log.status());
            System.out.println("금액: " + log.amount());
            System.out.println("메시지: " + log.message());
            System.out.println("주문번호: " + log.orderId());
            System.out.println("결제수단: " + log.payment().method());
            System.out.println("결제상태: " + log.payment().status());
            System.out.println("결제금액: " + log.payment().amount());
        });
    }
}
