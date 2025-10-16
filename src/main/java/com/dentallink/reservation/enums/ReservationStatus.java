package com.dentallink.reservation.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    PENDING,    // 대기
    APPROVED,   // 예약승인
    REJECTED,   // 거부
    CANCELLED,  // 취소
    COMPLETED  // 진료완료
}
