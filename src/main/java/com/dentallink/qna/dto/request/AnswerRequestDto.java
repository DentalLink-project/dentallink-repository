package com.dentallink.qna.dto.request;

public class AnswerRequestDto {

    // 답변 등록 요청 DTO answer create request dto
    public record AnswerCreateRequest(
            Long questionId,
            String content
    ) {
        public static AnswerCreateRequest of(Long questionId, String content) {
            return new AnswerCreateRequest(questionId, content);
        }
    }

    // 답변 수정 요청 DTO answer update request dto
    public record AnswerUpdateRequest(
            String content
    ) {
        public static AnswerUpdateRequest of(String content) {
            return new AnswerUpdateRequest(content);
        }
    }
}
