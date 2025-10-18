package com.dentallink.domain.payment.dto.request;

import lombok.NonNull;

public record PaymentRequest (
        @NonNull Long amount
){
}
