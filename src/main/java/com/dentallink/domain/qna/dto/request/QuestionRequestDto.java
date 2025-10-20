package com.dentallink.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class QuestionRequestDto {

    // 질문 등록 요청 DTO question create request dto
    public record QuestionCreateRequest(
            @NotNull Long hospitalId,
            @NotBlank String title,
            @NotBlank String content
    ) {}

    // 질문 수정 요청 DTO question update request dto
    public record QuestionUpdateRequest(
            @NotBlank String title,
            @NotBlank String content
    ) {}
}
