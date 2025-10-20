package com.dentallink.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AnswerRequestDto {

    // 답변 등록 요청 DTO answer create request dto
    public record AnswerCreateRequest(
            @NotNull Long questionId,
            @NotBlank String content
    ) {}

    // 답변 수정 요청 DTO answer update request dto
    public record AnswerUpdateRequest(
            @NotBlank String content
    ) {}
}
