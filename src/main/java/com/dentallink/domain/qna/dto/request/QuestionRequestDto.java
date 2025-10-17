package com.dentallink.domain.qna.dto.request;

public class QuestionRequestDto {

    // 질문 등록 요청 DTO question create request dto
    public record QuestionCreateRequest(
            Long hospitalId,
            String title,
            String content
    ) {
        public static QuestionCreateRequest of(Long hospitalId, String title, String content) {
            return new QuestionCreateRequest(hospitalId, title, content);
        }
    }

    // 질문 수정 요청 DTO question update request dto
    public record QuestionUpdateRequest(
            String title,
            String content
    ) {
        public static QuestionUpdateRequest of(String title, String content) {
            return new QuestionUpdateRequest(title, content);
        }
    }
}
