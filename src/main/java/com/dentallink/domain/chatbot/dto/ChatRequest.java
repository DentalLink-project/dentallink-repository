package com.dentallink.domain.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(

        Long sessionId,

        @NotBlank
        @Size(max = 2000, message = "메시지는 최대 2000자까지 입력 가능합니다.")
        String content
) {
}
