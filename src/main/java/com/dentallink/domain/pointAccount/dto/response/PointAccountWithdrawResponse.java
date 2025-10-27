package com.dentallink.domain.pointAccount.dto.response;

import com.dentallink.domain.pointAccount.dto.request.PointAccountWithdrawRequest;
import com.dentallink.domain.pointAccount.entity.PointAccount;
import com.dentallink.domain.pointAccount.enums.PointAccountType;
import java.time.LocalDateTime;

public record PointAccountWithdrawResponse(
        PointAccountType type,      // 출금 타입 (WITHDRAW)
        Long amount,                // 출금 금액
        Long balance,               // 출금 후 잔액
        String bankName,            // 출금 은행명
        String accountNumber,       // 계좌번호
        String accountHolder,       // 예금주명
        LocalDateTime createdAt     // 계좌 생성일 (또는 출금 시각)
) {
    public static PointAccountWithdrawResponse from(PointAccount account, PointAccountWithdrawRequest request) {
        return new PointAccountWithdrawResponse(
                PointAccountType.WITHDRAW,
                request.amount(),
                account.getBalance(),
                request.bankName(),
                request.accountNumber(),
                request.accountHolder(),
                account.getCreatedAt()
        );
    }
}
