package com.dentallink.domain.paymentLog.controller;

import com.dentallink.common.response.CommonApiResponse;
import com.dentallink.common.response.PageResponse;
import com.dentallink.domain.paymentLog.dto.response.PaymentLogResponse;
import com.dentallink.domain.paymentLog.service.PaymentLogInternalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "결제 로그 관리", description = "결제 상태 변화(READY, SUCCESS, CANCELLED 등) 이력 조회 API")
@RestController
@RequestMapping("/api/payment-log")
@RequiredArgsConstructor
@Validated
public class PaymentLogController {

    private final PaymentLogInternalService paymentLogInternalService;

    @Operation(
            summary = "주문별 결제 로그 조회",
            description = """
                    특정 주문(orderId)에 대한 결제 로그 목록을 페이지 단위로 조회합니다.
                    - 각 주문(orderId)은 하나의 결제(Payment)에 대응합니다.
                    - 하지만 하나의 결제는 여러 단계의 로그(READY, SUCCESS, CANCELLED 등)를 가질 수 있습니다.  
                    - 최신순(`sort=latest`) 또는 오래된순(`sort=oldest`) 정렬을 지원합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 주문의 결제를 찾을 수 없음")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<CommonApiResponse<PageResponse<PaymentLogResponse>>> getLogsByOrderId(
            @Parameter(description = "주문 ID (예: ORDER-20251103-ABC123)", example = "ORDER-20251103-ABC123")
            @PathVariable String orderId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @Parameter(description = "정렬 기준 (latest or oldest)", example = "latest")
            @RequestParam(defaultValue = "latest") String sort
    ) {
        PageResponse<PaymentLogResponse> response = paymentLogInternalService.getLogsByOrderId(orderId, page, size, sort);
        return CommonApiResponse.success(response, "결제 로그 조회 성공");
    }
}
