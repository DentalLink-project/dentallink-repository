package com.dentallink.domain.qna.dto.request;

public class QuestionRequestDto {

    // 질문 등록 요청 DTO question create request dto
    public record QuestionCreateRequest(Long hospitalId, String title, String content) {}

    // 질문 수정 요청 DTO question update request dto
    public record QuestionUpdateRequest(String title, String content) {}
}
