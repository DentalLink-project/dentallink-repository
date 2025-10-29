package com.dentallink.domain.chatbot.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;
/*
- `GeminiFunction` 자체는 **데이터를 안 가져서** record일 필요 없음
- **네임스페이스 역할**만 함 (관련 record들 그룹화)
- 하지만 **인스턴스화는 막아야 함** → `private 생성자` 추가!
 */

public final class GeminiFunction {

    private GeminiFunction() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    @Builder
    public record FunctionDeclaration(
            String name,
            String description,
            Map<String, Object> parameters
    ) {
    }

    @Builder
    public record FunctionCall(
            String name,
            Map<String, Object> arguments
    ) {
    }

    @Builder
    public record FunctionResponse(
            String name,
            Object response
    ) {
    }

    @Builder
    public record GeminiMessage(
            String role,
            String content,
            List<FunctionCall> functionCalls,
            List<FunctionResponse> functionResponses
    ) {
    }
}