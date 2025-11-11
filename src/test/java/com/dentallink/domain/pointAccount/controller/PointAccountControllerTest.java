package com.dentallink.domain.pointAccount.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.domain.pointAccount.dto.request.PointAccountDepositRequest;
import com.dentallink.domain.pointAccount.dto.request.PointAccountWithdrawRequest;
import com.dentallink.domain.pointAccount.dto.response.*;
import com.dentallink.domain.pointAccount.enums.PointAccountType;
import com.dentallink.domain.pointAccount.service.PointAccountInternalService;
import com.dentallink.domain.user.dto.security.AuthUser;
import com.dentallink.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointAccountControllerTest {

    @Mock
    private PointAccountInternalService pointAccountInternalService;

    @InjectMocks
    private PointAccountController pointAccountController;

    @Test
    @DisplayName("포인트 계좌 잔액 조회 성공")
    void getPointAccount_success() {
        // given
        AuthUser mockUser = new AuthUser(1L, "user@example.com", UserRole.ROLE_USER);
        PointAccountGetResponse mockResponse = new PointAccountGetResponse(10000L, LocalDateTime.now());
        when(pointAccountInternalService.getPointAccount(anyLong()))
                .thenReturn(mockResponse);

        // when
        ResponseEntity<CommonApiResponse<PointAccountGetResponse>> response =
                pointAccountController.getPointAccount(mockUser);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        CommonApiResponse<PointAccountGetResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getData().balance()).isEqualTo(10000L);
        assertThat(body.getMessage()).contains("잔액을 확인하였습니다.");

        System.out.println("잔액조회 상태코드: " + response.getStatusCode());
        System.out.println("잔액조회 잔액: " + body.getData().balance());
        System.out.println("잔액조회 메시지: " + body.getMessage() + "\n");
    }

    @Test
    @DisplayName("포인트 충전 성공 (관리자용)")
    void depositPointAccount_success() {
        // given
        PointAccountDepositRequest request = new PointAccountDepositRequest(1L, 5000L);
        PointAccountDepositResponse mockResponse = new PointAccountDepositResponse(
                PointAccountType.DEPOSIT,
                5000L,
                15000L,
                LocalDateTime.now()
        );
        when(pointAccountInternalService.depositPointAccount(anyLong(), anyLong()))
                .thenReturn(mockResponse);

        // when
        ResponseEntity<CommonApiResponse<PointAccountDepositResponse>> response =
                pointAccountController.depositPointAccount(request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        CommonApiResponse<PointAccountDepositResponse> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getData().type()).isEqualTo(PointAccountType.DEPOSIT);
        assertThat(body.getData().balance()).isEqualTo(15000L);
        assertThat(body.getMessage()).contains("포인트 충전에 성공했습니다.");

        System.out.println("포인트충전 상태코드: " + response.getStatusCode());
        System.out.println("포인트충전 충전금액: " + body.getData().amount());
        System.out.println("포인트충전 충전 후 잔액: " + body.getData().balance());
        System.out.println("포인트충전 메시지: " + body.getMessage() + "\n");

    }

    @Test
    @DisplayName("포인트 출금 요청 성공 (사용자용)")
    void withdrawPointAccount_success() {
        // given
        AuthUser mockUser = new AuthUser(1L, "user@example.com", UserRole.ROLE_USER);

        PointAccountWithdrawRequest request = new PointAccountWithdrawRequest(
                5000L,
                "신한은행",
                "123-456-7890",
                "홍길동"
        );

        PointAccountWithdrawResponse mockResponse = new PointAccountWithdrawResponse(
                PointAccountType.WITHDRAW,
                5000L,
                5000L,
                "신한은행",
                "123-456-7890",
                "홍길동",
                LocalDateTime.now()
        );

        when(pointAccountInternalService.withdrawPointAccount(anyLong(), any(PointAccountWithdrawRequest.class)))
                .thenReturn(mockResponse);

        // when
        ResponseEntity<CommonApiResponse<PointAccountWithdrawResponse>> response =
                pointAccountController.withdrawPointAccount(mockUser, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        CommonApiResponse<PointAccountWithdrawResponse> body = response.getBody();
        assertThat(body).isNotNull();

        PointAccountWithdrawResponse data = body.getData();
        assertThat(data.type()).isEqualTo(PointAccountType.WITHDRAW);
        assertThat(data.amount()).isEqualTo(5000L);
        assertThat(data.bankName()).isEqualTo("신한은행");
        assertThat(data.accountHolder()).isEqualTo("홍길동");
        assertThat(body.getMessage()).contains("포인트 출금 요청이 접수되었습니다.");

        System.out.println("포인트출금 상태코드: " + response.getStatusCode());
        System.out.println("포인트출금 은행: " + data.bankName());
        System.out.println("포인트출금 계좌번호: " + data.accountNumber());
        System.out.println("포인트출금 예금주: " + data.accountHolder());
        System.out.println("포인트출금 출금금액: " + data.amount());
        System.out.println("포인트출금 메시지: " + body.getMessage());
    }
}
