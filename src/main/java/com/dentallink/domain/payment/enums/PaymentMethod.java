package com.dentallink.domain.payment.enums;

public enum PaymentMethod {
    CARD("카드"),
    CASH("현금"),
    EASY_PAY("간편결제"),
    VIRTUAL_ACCOUNT("가상계좌"),
    ACCOUNT_TRANSFER("계좌이체");

    private final String korName;

    PaymentMethod(String korName) {
        this.korName = korName;
    }

    public static PaymentMethod from(String text) {
        for (PaymentMethod method : values()) {
            if (method.name().equalsIgnoreCase(text) || method.korName.equals(text)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown payment method: " + text);
    }
}

