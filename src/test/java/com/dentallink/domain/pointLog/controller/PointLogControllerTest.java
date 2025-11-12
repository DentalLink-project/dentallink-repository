package com.dentallink.domain.pointLog.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.pointLog.dto.response.PointLogPointAccountResponse;
import com.dentallink.domain.pointLog.dto.response.PointLogResponse;
import com.dentallink.domain.pointLog.enums.PointLogType;
import com.dentallink.domain.pointLog.service.PointLogInternalService;
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
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointLogControllerTest {

    @Mock
    private PointLogInternalService pointLogInternalService;

    @InjectMocks
    private PointLogController pointLogController;

    @Test
    @DisplayName("내 포인트 로그 조회 성공")
    void getMyLogs_success() {
        // given
        AuthUser mockUser = new AuthUser(1L, "user@example.com", UserRole.ROLE_USER);

        PointLogPointAccountResponse accountInfo = new PointLogPointAccountResponse(1L, 9500L);

        PointLogResponse log1 = new PointLogResponse(
                1L, PointLogType.DEPOSIT, 1000L, 10000L, accountInfo
        );
        PointLogResponse log2 = new PointLogResponse(
                2L, PointLogType.SPEND, 500L, 9500L, accountInfo
        );

        PageResponse<PointLogResponse> mockPage = new PageResponse<>(
                List.of(log1, log2),
                2L,
                1,
                10,
                0
        );

        when(pointLogInternalService.getLogsByUser(anyLong(), anyInt(), anyInt(), anyString(), any(), any()))
                .thenReturn(mockPage);

        // when
        ResponseEntity<CommonApiResponse<PageResponse<PointLogResponse>>> response =
                pointLogController.getMyLogs(mockUser, 0, 10, "latest", null, null);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        CommonApiResponse<PageResponse<PointLogResponse>> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getData().getContent()).hasSize(2);
        assertThat(body.getMessage()).contains("내 포인트 로그 조회 성공");

        System.out.println("포인트로그조회 성공 테스트");
        System.out.println("상태코드: " + response.getStatusCode());
        System.out.println("로그 개수: " + body.getData().getContent().size());
        body.getData().getContent().forEach(log -> {
            System.out.println("로그 ID: " + log.id());
            System.out.println("타입: " + log.type());
            System.out.println("금액: " + log.amount());
            System.out.println("잔액(이후): " + log.balanceAfter());
            System.out.println("계좌 ID: " + log.account().pointAccountId());
            System.out.println("계좌 잔액: " + log.account().balance() + "\n");
        });
    }

    @Test
    @DisplayName("조회 시작일시가 종료일시보다 늦을 경우 예외 발생")
    void getMyLogs_invalidDateRange() {
        // given
        AuthUser mockUser = new AuthUser(1L, "user@example.com", UserRole.ROLE_USER);
        LocalDateTime startDate = LocalDateTime.of(2025, 11, 10, 10, 0);
        LocalDateTime endDate = LocalDateTime.of(2025, 11, 9, 10, 0);

        // when & then
        assertThatThrownBy(() ->
                pointLogController.getMyLogs(mockUser, 0, 10, "latest", startDate, endDate)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("조회 시작일시는 종료일시보다 늦을 수 없습니다.");

        System.out.println("포인트로그조회 실패 테스트");
        System.out.println("시작일(" + startDate + ")이 종료일(" + endDate + ")보다 늦어 예외 발생 확인");
    }
}
