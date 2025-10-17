package com.dentallink.domain.qna.dto.request;

public class AnswerRequestDto {

    // 답변 등록 요청 DTO answer create request dto
    public record AnswerCreateRequest(Long questionId, String content) {}

    // 답변 수정 요청 DTO answer update request dto
    public record AnswerUpdateRequest(String content) {}
}
